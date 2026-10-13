package com.cityscape.geoszabaduloszobabackend;

import com.cityscape.geoszabaduloszobabackend.api.mapper.ApiMapper;
import com.cityscape.geoszabaduloszobabackend.model.domain.*;
import com.cityscape.geoszabaduloszobabackend.model.dto.PageDTO;
import com.cityscape.geoszabaduloszobabackend.model.entity.*;
import com.cityscape.geoszabaduloszobabackend.model.enums.Difficulty;
import com.cityscape.geoszabaduloszobabackend.service.SearchPagination;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

class ApiContractTest {
    private final ObjectMapper json = new ObjectMapper().findAndRegisterModules();
    private final ApiMapper mapper = new ApiMapper(json);

    @Test
    void detailMappingPreservesDefaultsTimeAndInvalidStationFallback() {
        var adventure = new AdventureEntity();
        adventure.setId(7L);
        adventure.setTitle("Kaland");
        adventure.setDifficulty(Difficulty.EASY);
        adventure.setAverageTimeInSeconds(3661);
        var start = station(0, null);
        var playable = station(1, "invalid-json");
        var dto = mapper.toDto(new AdventureDetails(adventure, List.of(start, playable), List.of(), "creator", null));
        assertThat(dto.getAverageTime()).isEqualTo("1 h 1 m 1 s");
        assertThat(dto.getAverageRating()).isEqualTo(0.0);
        assertThat(dto.getDifficulty()).isEqualTo("EASY");
        assertThat(dto.isHasStartingPoint()).isTrue();
        assertThat(dto.getStations().get(0).content()).isNull();
        assertThat(dto.getStations().get(1).content().getRiddle()).isEqualTo("Hiba a tartalom betöltésekor");
        assertThat(dto.getRatingDistribution()).isNull();
    }

    @Test
    void editMappingFindsLastPlayableStationAndKeepsMalformedContentFailure() {
        var adventure = new AdventureEntity();
        var first = station(1, "{\"riddle\":\"R\",\"hints\":[]}");
        var last = station(2, " ");
        var dto = mapper.toDto(new AdventureEdit(adventure, List.of(last, station(0, null), first)));
        assertThat(dto.getDifficulty()).isEqualTo("EASY");
        assertThat(dto.getStations().get(0).isLastStation()).isTrue();
        assertThat(dto.getStations().get(1).isLastStation()).isFalse();
        assertThat(dto.getStations().get(2).content().getRiddle()).isEqualTo("R");
        assertThatThrownBy(() -> mapper.toDto(new AdventureEdit(adventure, List.of(station(1, "bad-json")))))
                .isInstanceOf(RuntimeException.class).hasMessage("Hiba a StationContent JSON-nal");
    }

    @Test
    void moderationJsonAndCreatedAdventureNullModerationStayCompatible() throws Exception {
        var domain = json.readValue("{\"isProfane\":true,\"isSolvable\":false,\"reason\":\"reason\"}", ModerationResult.class);
        assertThat(domain.isProfane()).isTrue();
        assertThat(domain.isSolvable()).isFalse();
        var entity = new AiModerationResponseEntity();
        entity.setProfane(true);
        entity.setReason("reason");
        var adventure = new AdventureEntity();
        var dto = mapper.toDto(new CreatedAdventure(adventure, entity));
        var response = json.valueToTree(dto.aiModeration());
        assertThat(response.path("isProfane").asBoolean()).isTrue();
        assertThat(response.path("reason").asText()).isEqualTo("reason");
        assertThat(mapper.toDto(new CreatedAdventure(adventure, null)).aiModeration()).isNull();
    }

    @Test
    void pageMetadataAndInvalidParametersHaveExplicitContracts() {
        var page = new PageImpl<>(List.of("item"), SearchPagination.request(1, 1), 3);
        var dto = PageDTO.from(page);
        assertThat(dto.page()).isEqualTo(1);
        assertThat(dto.totalElements()).isEqualTo(3);
        assertThat(dto.totalPages()).isEqualTo(3);
        assertThat(dto.first()).isFalse();
        assertThat(dto.last()).isFalse();
        for (int[] invalid : List.of(new int[]{-1, 20}, new int[]{0, 0}, new int[]{0, 101})) {
            assertThatThrownBy(() -> SearchPagination.request(invalid[0], invalid[1]))
                    .isInstanceOf(ResponseStatusException.class)
                    .satisfies(error -> assertThat(((ResponseStatusException) error).getStatusCode().value()).isEqualTo(400));
        }
    }

    @Test
    void reviewAndCompletedResponsesKeepAdventureIdsAndNullRatings() {
        var adventure = new AdventureEntity();
        adventure.setId(9L);
        adventure.setTitle("Title");
        var review = new ReviewEntity();
        review.setId(3L);
        review.setAdventure(adventure);
        review.setRating(4);
        review.setReviewText("Text");
        var reviewDto = mapper.toDto(review);
        assertThat(reviewDto.adventureId()).isEqualTo(9L);
        assertThat(reviewDto.title()).isEqualTo("Title");
        assertThat(reviewDto.rating()).isEqualTo(4);
        var completed = new CompletedAdventureEntity();
        completed.setId(5L);
        completed.setAdventure(adventure);
        completed.setPoints(17);
        assertThat(mapper.toDto(new CompletedAdventure(completed, null)).rating()).isNull();
        assertThat(mapper.toDto(new CompletedAdventure(completed, review)).rating()).isEqualTo(4);
        assertThat(mapper.toDto(new CompletedAdventure(completed, review)).points()).isEqualTo(17);
    }

    @Test
    void listMappingPreservesAdventureIdsAndReviewListsUseReviewText() {
        var adventure = new AdventureEntity();
        adventure.setId(9L);
        adventure.setDescription("Description");
        var list = new ListEntity();
        list.setId(2L);
        list.setTitle("List");
        list.setAdventures(List.of(adventure));
        var dto = mapper.toDto(list);
        assertThat(dto.id()).isEqualTo(2L);
        assertThat(dto.adventureIds()).containsExactly(9L);
        assertThat(mapper.toListDetailsDto(list).adventures().get(0).description()).isEqualTo("Description");
        assertThat(mapper.toDto(new ReviewedAdventure(adventure, "Review text")).description()).isEqualTo("Review text");
    }

    private StationEntity station(int sequence, String content) {
        var station = new StationEntity();
        station.setSeqNumber(sequence);
        station.setContent(content);
        return station;
    }
}
