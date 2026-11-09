package com.cityscape.geoszabaduloszobabackend;

import com.cityscape.geoszabaduloszobabackend.model.entity.*;
import com.cityscape.geoszabaduloszobabackend.repository.AccountDeletionRepository;
import com.cityscape.geoszabaduloszobabackend.repository.AccountDataRepository;
import com.cityscape.geoszabaduloszobabackend.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;
import javax.sql.DataSource;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@DataJpaTest(properties = {
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
        "spring.datasource.password=", "spring.jpa.show-sql=false"
})
class AccountServiceTest {
    @Autowired TestEntityManager entities;
    @Autowired AccountDeletionRepository deletions;
    @Autowired DataSource dataSource;
    @Autowired PlatformTransactionManager transactionManager;
    @Autowired AccountDataRepository accountData;
    UserService users;
    KeycloakAdminService keycloak;
    AvatarStorageService avatars;
    AccountService service;
    JdbcTemplate jdbc;

    @BeforeEach
    void setup() {
        users = mock(UserService.class);
        keycloak = mock(KeycloakAdminService.class);
        avatars = mock(AvatarStorageService.class);
        when(users.getCurrentUserId()).thenReturn("owner");
        jdbc = new JdbcTemplate(dataSource);
        service = new AccountService(users, keycloak, deletions, avatars, accountData,
                new TransactionTemplate(transactionManager));
    }

    @Test
    void anonymizesPublicContentAndPreservesReviewsAndOtherPlayersHistory() throws Exception {
        var owner = user("owner");
        var other = user("other");
        var owned = adventure(owner, "Owned");
        var surviving = adventure(other, "Surviving");
        var draft = adventure(owner, "Draft");
        draft.setStatus("DRAFT");
        var draftStation = new StationEntity();
        draftStation.setAdventure(draft);
        draftStation.setSeqNumber(1);
        entities.persist(draftStation);
        var station = new StationEntity();
        station.setAdventure(owned);
        station.setSeqNumber(1);
        entities.persist(station);
        var list = new ListEntity();
        list.setCreator(other);
        list.setTitle("Other list");
        list.getAdventures().add(owned);
        list.getAdventures().add(surviving);
        entities.persist(list);
        var ownList = new ListEntity();
        ownList.setCreator(owner);
        ownList.setTitle("Own list");
        ownList.getAdventures().add(surviving);
        entities.persist(ownList);
        var follow = new FollowEntity();
        follow.setFollower(other);
        follow.setFollowed(owner);
        entities.persist(follow);
        review(owner, surviving, 1);
        review(other, surviving, 5);
        review(other, owned, 3);
        var completed = new CompletedAdventureEntity();
        completed.setUser(other);
        completed.setAdventure(owned);
        entities.persist(completed);
        var abandoned = new AbandonedAdventureEntity();
        abandoned.setUser(other);
        abandoned.setAdventure(owned);
        entities.persist(abandoned);
        var moderation = new ModerationResponseEntity();
        moderation.setAdventure(owned);
        moderation.setReason("Reason");
        entities.persist(moderation);
        var ai = new AiModerationResponseEntity();
        ai.setAdventure(owned);
        entities.persist(ai);
        entities.flush();

        jdbc.update("update adventure set average_rating = ? where id = ?", 3.0, surviving.getId());
        service.deleteAccount("TÖRLÉS");
        entities.flush();
        entities.clear();

        assertThat(jdbc.queryForObject("select count(*) from users", Long.class)).isEqualTo(2);
        assertThat(jdbc.queryForObject("select count(*) from adventure", Long.class)).isEqualTo(2);
        assertThat(jdbc.queryForObject("select count(*) from list_adventures", Long.class)).isEqualTo(2);
        assertThat(jdbc.queryForObject("select average_rating from adventure where id = ?", Double.class,
                surviving.getId())).isEqualTo(3.0);
        assertThat(jdbc.queryForObject("select count(*) from review", Long.class)).isEqualTo(3);
        assertThat(jdbc.queryForObject("select count(*) from user_follows", Long.class)).isZero();
        assertThat(jdbc.queryForObject("select count(*) from users where keycloak_sub = 'owner'", Long.class)).isZero();
        Long anonymousId = jdbc.queryForObject("select id from users where keycloak_sub = ?", Long.class, UserEntity.DELETED_SUBJECT);
        assertThat(jdbc.queryForObject("select creator_id from adventure where id = ?", Long.class, owned.getId())).isEqualTo(anonymousId);
        assertThat(jdbc.queryForObject("select count(*) from review where user_id = ?", Long.class, anonymousId)).isEqualTo(1);
        for (String table : new String[]{"station", "completed_adventure", "abandoned_adventure",
                "moderation_response", "ai_moderation_response"}) {
            assertThat(jdbc.queryForObject("select count(*) from " + table, Long.class)).isEqualTo(1);
        }
        assertThat(deletions.findById("owner").orElseThrow().isCompleted()).isTrue();
        verify(avatars).deleteUserAvatars(owner.getId());
        verify(keycloak).deleteAccount("owner");
    }

    @Test
    void multipleDeletedUsersShareOneAnonymousOwnerWithoutLosingTheirRatings() {
        var owner = user("owner");
        var other = user("other");
        var adventure = adventure(other, "Public");
        review(owner, adventure, 1);
        review(other, adventure, 5);
        entities.flush();
        service.deleteAccount("TÖRLÉS");
        entities.flush();
        entities.clear();
        when(users.getCurrentUserId()).thenReturn("other");
        service.deleteAccount("TÖRLÉS");
        entities.flush();
        assertThat(jdbc.queryForObject("select count(*) from users", Long.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("select count(*) from review", Long.class)).isEqualTo(2);
        assertThat(jdbc.queryForObject("select avg(rating) from review", Double.class)).isEqualTo(3.0);
    }

    @Test
    void keepsFailedDeletionForRetryAndRequiresExplicitConfirmation() {
        assertThatThrownBy(() -> service.deleteAccount("wrong")).isInstanceOf(ResponseStatusException.class);
        assertThat(deletions.existsById("owner")).isFalse();
        doThrow(new IllegalStateException("offline")).doNothing().when(keycloak).deleteAccount("owner");
        service.deleteAccount("TÖRLÉS");
        assertThat(deletions.findById("owner").orElseThrow().isCompleted()).isFalse();
        service.retryDeletions();
        assertThat(deletions.findById("owner").orElseThrow().isCompleted()).isTrue();
    }

    @Test
    void rateLimitsEmailAndDoesNotSendForVerifiedAccounts() {
        var representation = new UserRepresentation();
        representation.setEmail("owner@example.com");
        representation.setEmailVerified(false);
        when(keycloak.getById("owner")).thenReturn(representation);
        service.verifyEmail();
        assertThatThrownBy(service::verifyEmail).isInstanceOf(ResponseStatusException.class)
                .satisfies(error -> assertThat(((ResponseStatusException) error).getStatusCode().value()).isEqualTo(429));
        representation.setEmailVerified(true);
        service.verifyEmail();
        verify(keycloak, times(1)).sendVerificationEmail("owner");
    }

    private UserEntity user(String sub) {
        var user = new UserEntity();
        user.setKeycloakSub(sub);
        return entities.persist(user);
    }

    private AdventureEntity adventure(UserEntity creator, String title) {
        var adventure = new AdventureEntity();
        adventure.setCreator(creator);
        adventure.setTitle(title);
        adventure.setStatus("PUBLIC");
        return entities.persist(adventure);
    }

    private void review(UserEntity user, AdventureEntity adventure, int rating) {
        var review = new ReviewEntity();
        review.setUser(user);
        review.setAdventure(adventure);
        review.setRating(rating);
        entities.persist(review);
    }
}
