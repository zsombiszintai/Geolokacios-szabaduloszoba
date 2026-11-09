package com.cityscape.geoszabaduloszobabackend;

import com.cityscape.geoszabaduloszobabackend.api.mapper.ApiMapper;
import com.cityscape.geoszabaduloszobabackend.model.domain.AdventureDetails;
import com.cityscape.geoszabaduloszobabackend.model.entity.*;
import com.cityscape.geoszabaduloszobabackend.repository.*;
import com.cityscape.geoszabaduloszobabackend.service.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@DataJpaTest(properties = {
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
        "spring.datasource.password=", "spring.jpa.show-sql=false"
})
class ReviewServiceTest {
    @Autowired TestEntityManager entities;
    @Autowired ReviewRepository reviews;
    @Autowired UserRepository users;
    @Autowired AdventureRepository adventures;

    @Test
    void createsThenUpdatesTheSameRatingWithoutCalculatingAverage() {
        var user = user("active");
        var deleted = user(UserEntity.DELETED_SUBJECT);
        var adventure = new AdventureEntity();
        adventure.setCreator(user);
        adventure.setTitle("Rated adventure");
        entities.persist(adventure);
        var anonymousReview = rating(1);
        anonymousReview.setUser(deleted);
        anonymousReview.setAdventure(adventure);
        entities.persist(anonymousReview);
        entities.flush();
        var current = mock(UserService.class);
        when(current.getCurrentUserId()).thenReturn("active");
        var service = new ReviewService(reviews, users, adventures, current);
        service.saveReview(adventure.getId(), rating(5));
        assertThat(adventure.getAverageRating()).isNull();
        Long reviewId = reviews.findByAdventureIdAndUserIdOrderByIdDesc(adventure.getId(), user.getId()).get(0).getId();
        service.saveReview(adventure.getId(), rating(3));
        assertThat(adventure.getAverageRating()).isNull();
        assertThat(reviews.findByAdventureId(adventure.getId())).hasSize(2);
        assertThat(reviews.findByAdventureIdAndUserIdOrderByIdDesc(adventure.getId(), user.getId()).get(0).getId()).isEqualTo(reviewId);

        var mapper = new ApiMapper(new ObjectMapper());
        adventure.setAverageRating(4.5); // The API forwards the database value, independently of the reviews.
        var dto = mapper.toDto(new AdventureDetails(adventure, List.of(),
                reviews.findByAdventureId(adventure.getId()), UserEntity.DELETED_NAME, null));
        assertThat(dto.getAverageRating()).isEqualTo(4.5);
        assertThat(dto.getReviews()).anySatisfy(review -> assertThat(review.authorName()).isEqualTo(UserEntity.DELETED_NAME));
        assertThatThrownBy(() -> service.saveReview(adventure.getId(), rating(6)))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void repairsOldDuplicateRatingsWhenTheUserUpdatesTheirReview() {
        var user = user("active");
        var adventure = new AdventureEntity();
        adventure.setCreator(user);
        adventure.setTitle("Duplicates");
        entities.persist(adventure);
        for (int value : new int[]{1, 5}) {
            var review = rating(value);
            review.setUser(user);
            review.setAdventure(adventure);
            entities.persist(review);
        }
        entities.flush();
        var current = mock(UserService.class);
        when(current.getCurrentUserId()).thenReturn("active");
        new ReviewService(reviews, users, adventures, current).saveReview(adventure.getId(), rating(4));
        assertThat(reviews.findByAdventureId(adventure.getId())).hasSize(1);
        assertThat(adventure.getAverageRating()).isNull();
    }

    private UserEntity user(String sub) {
        var user = new UserEntity();
        user.setKeycloakSub(sub);
        return entities.persist(user);
    }

    private ReviewEntity rating(int value) {
        var review = new ReviewEntity();
        review.setRating(value);
        return review;
    }
}
