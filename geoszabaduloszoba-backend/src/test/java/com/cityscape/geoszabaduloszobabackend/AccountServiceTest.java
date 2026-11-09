package com.cityscape.geoszabaduloszobabackend;

import com.cityscape.geoszabaduloszobabackend.model.entity.*;
import com.cityscape.geoszabaduloszobabackend.repository.AccountDeletionRepository;
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
        service = new AccountService(users, keycloak, deletions, avatars, jdbc,
                new TransactionTemplate(transactionManager));
    }

    @Test
    void removesDependentDataButPreservesOtherUsersAndTheirAdventures() throws Exception {
        var owner = user("owner");
        var other = user("other");
        var owned = adventure(owner, "Owned");
        var surviving = adventure(other, "Surviving");
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

        service.deleteAccount("TÖRLÉS");
        entities.flush();
        entities.clear();

        assertThat(jdbc.queryForObject("select count(*) from users", Long.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("select count(*) from adventure", Long.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("select count(*) from list_adventures", Long.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("select average_rating from adventure where id = ?", Double.class,
                surviving.getId())).isEqualTo(5.0);
        for (String table : new String[]{"station", "completed_adventure", "abandoned_adventure",
                "user_follows", "moderation_response", "ai_moderation_response"}) {
            assertThat(jdbc.queryForObject("select count(*) from " + table, Long.class)).isZero();
        }
        assertThat(deletions.findById("owner").orElseThrow().isCompleted()).isTrue();
        verify(avatars).deleteUserAvatars(owner.getId());
        verify(keycloak).deleteAccount("owner");
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
