package com.cityscape.geoszabaduloszobabackend;

import com.cityscape.geoszabaduloszobabackend.model.entity.AdventureEntity;
import com.cityscape.geoszabaduloszobabackend.model.entity.CompletedAdventureEntity;
import com.cityscape.geoszabaduloszobabackend.model.entity.StationEntity;
import com.cityscape.geoszabaduloszobabackend.model.entity.UserEntity;
import com.cityscape.geoszabaduloszobabackend.model.enums.Difficulty;
import com.cityscape.geoszabaduloszobabackend.repository.AdventureRepository;
import com.cityscape.geoszabaduloszobabackend.service.SearchPagination;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(properties = {
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
        "spring.datasource.password=",
        "spring.jpa.show-sql=false"
})
class PagedSearchRepositoryTest {
    @Autowired AdventureRepository repository;
    @Autowired TestEntityManager entities;

    @Test
    void ignoresStationZeroAndCountsOnlyAdventuresWithUsableFirstStations() {
        var first = adventure("City first", "PUBLIC");
        station(first, 0, 47.0, 19.0);
        station(first, 1, 47.1, 19.1);
        var fallback = adventure("City fallback", "PUBLIC");
        station(fallback, 1, 47.2, 19.2);
        var invalid = adventure("City invalid", "PUBLIC");
        station(invalid, 0, 47.3, 19.3);
        station(invalid, 1, null, null);
        var hidden = adventure("City draft", "DRAFT");
        station(hidden, 1, 47.4, 19.4);
        adventure("City empty", "PUBLIC");
        entities.flush();

        var page = repository.searchPublicWithLocation("city", null, null, null, null, SearchPagination.request(0, 1));
        assertThat(page.getTotalElements()).isEqualTo(2);
        assertThat(page.getTotalPages()).isEqualTo(2);
        assertThat(page.getContent()).extracting(match -> match.getAdventure().getId()).containsExactly(first.getId());
        var next = repository.searchPublicWithLocation("city", null, null, null, null, SearchPagination.request(1, 1));
        assertThat(next.getContent()).extracting(match -> match.getAdventure().getId()).containsExactly(fallback.getId());
        assertThat(next.isLast()).isTrue();
        assertThat(repository.searchPublicWithLocation("city", null, null, null, null,
                SearchPagination.request(2, 1)).getContent()).isEmpty();
    }

    @Test
    void searchMatchesCaseInsensitivelyAndSupportsEscapedLiteralWildcards() {
        station(adventure("City 100%_done", "PUBLIC"), 1, 47.0, 19.0);
        station(adventure("City 100-other", "PUBLIC"), 1, 47.0, 19.0);
        entities.flush();
        var pageable = SearchPagination.request(0, 20);
        assertThat(repository.searchPublicWithLocation("CITY", null, null, null, null, pageable).getTotalElements()).isEqualTo(2);
        assertThat(repository.searchPublicWithLocation("100\\%\\_", null, null, null, null, pageable).getTotalElements()).isEqualTo(1);
        assertThat(repository.searchPublicWithLocation("", null, null, null, null, pageable).getTotalElements()).isEqualTo(2);
    }

    @Test
    void appliesDifficultyBeforePagingEvenWithoutAKeyword() {
        station(adventure("Unknown difficulty", "PUBLIC"), 1, 47.0, 19.0);
        var hard = adventure("Hard adventure", "PUBLIC");
        hard.setDifficulty(Difficulty.HARD);
        station(hard, 1, 47.0, 19.0);
        entities.flush();

        var page = repository.searchPublicWithLocation("", Difficulty.HARD, null, null, null, SearchPagination.request(0, 1));
        assertThat(page.getTotalElements()).isEqualTo(1);
        assertThat(page.getContent()).extracting(match -> match.getAdventure().getId()).containsExactly(hard.getId());
    }

    @Test
    void combinesAdvancedFiltersBeforePagingAndIncludesBoundaryValues() {
        var unrated = adventure("Unrated", "PUBLIC");
        station(unrated, 1, 47.0, 19.0);
        adventureWithStats("Too long", 4.5, 3_001, 3_600);
        adventureWithStats("Too slow", 4.5, 3_000, 3_601);
        adventureWithStats("Too low", 4.4, 3_000, 3_600);
        var match = adventureWithStats("Matches all filters", 4.5, 3_000, 3_600);

        var page = repository.searchPublicWithLocation("", null, 4.5, 3_000.0, 3_600,
                SearchPagination.request(0, 1));
        assertThat(page.getTotalElements()).isEqualTo(1);
        assertThat(page.getContent()).extracting(result -> result.getAdventure().getId()).containsExactly(match.getId());
        assertThat(page.getContent().get(0).getAverageDuration()).isEqualTo(3_600.0);
    }

    private AdventureEntity adventureWithStats(String title, double rating, double distance, int duration) {
        var adventure = adventure(title, "PUBLIC");
        adventure.setTotalDistance(distance);
        station(adventure, 1, 47.0, 19.0);
        entities.flush();
        entities.getEntityManager().createNativeQuery("""
                update adventure set average_rating = :rating where id = :id
                """)
                .setParameter("rating", rating)
                .setParameter("id", adventure.getId())
                .executeUpdate();
        var completion = new CompletedAdventureEntity();
        completion.setAdventure(adventure);
        completion.setUser(adventure.getCreator());
        completion.setDurationSec(duration);
        entities.persist(completion);
        return adventure;
    }

    private AdventureEntity adventure(String title, String status) {
        var user = new UserEntity();
        user.setKeycloakSub(title);
        entities.persist(user);
        var adventure = new AdventureEntity();
        adventure.setCreator(user);
        adventure.setTitle(title);
        adventure.setStatus(status);
        return entities.persist(adventure);
    }

    private void station(AdventureEntity adventure, int sequence, Double latitude, Double longitude) {
        var station = new StationEntity();
        station.setAdventure(adventure);
        station.setSeqNumber(sequence);
        station.setLatitude(latitude);
        station.setLongitude(longitude);
        entities.persist(station);
    }
}
