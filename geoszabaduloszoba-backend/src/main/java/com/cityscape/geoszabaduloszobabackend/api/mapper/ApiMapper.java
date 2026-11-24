package com.cityscape.geoszabaduloszobabackend.api.mapper;

import com.cityscape.geoszabaduloszobabackend.model.dto.*;
import com.cityscape.geoszabaduloszobabackend.model.domain.*;
import com.cityscape.geoszabaduloszobabackend.model.entity.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.stream.IntStream;

@Component
@RequiredArgsConstructor
public class ApiMapper {
    private static final ObjectReader STATION_CONTENT_READER = new ObjectMapper().readerFor(StationContent.class);
    private final ObjectMapper objectMapper;

    public NearbyAdventureDTO toDto(NearbyAdventureResult source) {
        return new NearbyAdventureDTO(source.id(), source.title(), source.distanceInMeters(),
                source.averageTime(), source.advLon(), source.advLat());
    }

    public SearchDTO toDto(SearchResult source) {
        return SearchDTO.builder().id(source.getId()).title(source.getTitle())
                .creator(source.getCreator()).type(source.getType()).lat(source.getLat()).lon(source.getLon()).build();
    }

    public AdventureListDTO toDto(AdventureEntity adventure) {
        return new AdventureListDTO(adventure.getId(), adventure.getTitle(), adventure.getDescription(), adventure.getDifficulty());
    }

    public AdventureListDTO toDto(ReviewedAdventure source) {
        var adventure = source.adventure();
        return new AdventureListDTO(adventure.getId(), adventure.getTitle(), source.reviewText(), adventure.getDifficulty());
    }

    public UserListDTO toDto(UserSummary source) {
        return new UserListDTO(source.id(), source.username(), source.description(), source.profilePictureUrl());
    }

    public ReviewDTO toDto(ReviewEntity review) {
        return new ReviewDTO(review.getId(), review.getAdventure().getId(), review.getAdventure().getTitle(),
                review.getRating(), review.getReviewText(), review.getReviewedAt(),
                UserEntity.DELETED_NAME);
    }

    public ReviewEntity toReviewEntity(ReviewDTO source) {
        var review = new ReviewEntity();
        review.setRating(source.rating());
        review.setReviewText(source.reviewText());
        return review;
    }

    public GameUpdate toGameUpdate(ActiveGameDTO source) {
        return new GameUpdate(source.sessionId(), source.lastStationId(), source.title(),
                source.elapsedSec(), source.distanceInMeters(), source.points());
    }

    public GameSessionDTO toDto(AbandonedAdventureEntity session) {
        return new GameSessionDTO(session.getId(), session.getAdventure().getId(), session.getLastStationId(),
                session.getElapsedSec(), session.getDistanceTravelled(), session.getPoints(), session.isCompleted());
    }

    public AbandonedAdventureDTO toDto(AbandonedAdventure source) {
        var session = source.session();
        return new AbandonedAdventureDTO(session.getId(), session.getAdventure().getId(), session.getAdventure().getTitle(),
                session.getLastStationId(), source.lastStationSequence(), session.getElapsedSec(),
                session.getDistanceTravelled(), session.getPoints());
    }

    public CompletedAdventureDTO toDto(CompletedAdventure source) {
        var completed = source.completed();
        return new CompletedAdventureDTO(completed.getId(), completed.getAdventure().getId(), completed.getAdventure().getTitle(),
                completed.getCompletedAt(), completed.getDistanceTravelled(), completed.getDurationSec(),
                source.review() == null ? null : source.review().getRating(), completed.getPoints());
    }

    public AdventureCreatedDTO toDto(CreatedAdventure source) {
        var adventure = source.adventure();
        return new AdventureCreatedDTO(adventure.getId(), adventure.getTitle(), adventure.getCreatedAt(),
                adventure.getStatus(), toDto(source.moderation()));
    }

    private AiModerationResponse toDto(AiModerationResponseEntity source) {
        if (source == null) return null;
        return new AiModerationResponse(source.isProfane(), source.isSolvable(), source.getProfanityDetails(),
                source.getSolvabilityDetails(), source.isOverallApproved(), source.getReason());
    }

    public AdventureCollectionDTO toDto(ListEntity list) {
        return new AdventureCollectionDTO(list.getId(), list.getTitle(), list.getDescription(),
                list.getAdventures().stream().map(AdventureEntity::getId).toList());
    }

    public ListDetailsDTO toListDetailsDto(ListEntity list) {
        return new ListDetailsDTO(list.getId(), list.getTitle(), list.getDescription(),
                list.getAdventures().stream().map(this::toDto).toList());
    }

    public AdventureProfileDTO toDto(AdventureDetails source) {
        var adventure = source.adventure();
        var dto = new AdventureProfileDTO();
        dto.setId(adventure.getId());
        dto.setTitle(adventure.getTitle());
        dto.setDescription(adventure.getDescription());
        dto.setAverageTime(formatTime(adventure.getAverageTimeInSeconds()));
        dto.setDistanceInMeters(source.routeDistance());
        dto.setDifficulty(adventure.getDifficulty() == null ? "UNKNOWN" : adventure.getDifficulty().name());
        dto.setCreatorName(source.creatorName());
        dto.setCreatorProfilePictureUrl(source.creatorAvatarUrl());
        dto.setAverageRating(adventure.getAverageRating() == null ? 0.0 : adventure.getAverageRating());
        dto.setReviews(source.reviews().stream().map(this::toDto).toList());
        var stations = source.stations();
        dto.setStations(IntStream.range(0, stations.size())
                .mapToObj(i -> toStationDto(stations.get(i), i == stations.size() - 1)).toList());
        return dto;
    }

    public AdventureCreateDTO toDto(AdventureEdit source) {
        var adventure = source.adventure();
        var dto = new AdventureCreateDTO();
        dto.setTitle(adventure.getTitle());
        dto.setDescription(adventure.getDescription());
        dto.setDifficulty(adventure.getDifficulty() == null ? "EASY" : adventure.getDifficulty().name());
        dto.setStatus(adventure.getStatus());
        var stations = source.stations();
        dto.setStations(IntStream.range(0, stations.size()).mapToObj(i -> {
            var station = stations.get(i);
            boolean last = i == stations.size() - 1;
            return new StationCreateDTO(station.getSeqNumber(), last ? null : readContentForEdit(station.getContent()),
                    station.getLatitude(), station.getLongitude(), last);
        }).toList());
        return dto;
    }

    private StationContent readContentForEdit(String json) {
        if (json == null || json.isBlank()) return null;
        try { return objectMapper.readValue(json, StationContent.class); }
        catch (Exception exception) { throw new RuntimeException("Hiba a StationContent JSON-nal"); }
    }

    private StationDTO toStationDto(StationEntity station, boolean last) {
        StationContent content = null;
        try {
            if (!last && station.getContent() != null) content = STATION_CONTENT_READER.readValue(station.getContent());
        } catch (Exception exception) {
            content = new StationContent("Hiba a tartalom betöltésekor", "", List.of());
        }
        return new StationDTO(station.getId(), station.getLatitude(), station.getLongitude(), content, station.getSeqNumber());
    }

    private String formatTime(Integer totalSeconds) {
        if (totalSeconds == null || totalSeconds == 0) return "0 s";
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;
        StringBuilder result = new StringBuilder();
        if (hours > 0) result.append(hours).append(" h ");
        if (minutes > 0 || hours > 0) result.append(minutes).append(" m ");
        if (seconds > 0 || (hours == 0 && minutes == 0)) result.append(seconds).append(" s");
        return result.toString().trim();
    }
}
