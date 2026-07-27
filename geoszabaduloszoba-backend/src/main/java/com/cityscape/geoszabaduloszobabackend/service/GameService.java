package com.cityscape.geoszabaduloszobabackend.service;

import com.cityscape.geoszabaduloszobabackend.mapper.AbstractMapper;
import com.cityscape.geoszabaduloszobabackend.model.dto.ActiveGameDTO;
import com.cityscape.geoszabaduloszobabackend.model.entity.*;
import com.cityscape.geoszabaduloszobabackend.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.mapstruct.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class GameService {

    private final AbandonedAdventureRepository abandonedRepository;
    private final CompletedAdventureRepository completedRepository;
    private final StationRepository stationRepository;
    private final UserRepository userRepository;
    private final AdventureRepository adventureRepository;
    private final GameServiceMapper gameServiceMapper;

    public void updateActiveGame(ActiveGameDTO dto) {

        var existing = abandonedRepository.findById(dto.sessionId())
                .orElseThrow(() -> new RuntimeException("Session nem található: " + dto.sessionId()));

        this.gameServiceMapper.mergeUpdate(existing, dto);

        abandonedRepository.save(existing);

        stationRepository.findById(dto.lastStationId()).ifPresent(station -> {
            if (station.isLastStation()) {
                finishGame(existing);
            }
        });
    }

    private void finishGame(AbandonedAdventureEntity abandoned) {
        var completed = gameServiceMapper.toCompleted(abandoned);
        completedRepository.save(completed);

        var user = abandoned.getUser();
        if (user != null) {

            int currentPoints = user.getPoints() != null ? user.getPoints() : 0;
            int earnedPoints = abandoned.getPoints() != null ? abandoned.getPoints() : 0;

            user.setPoints(currentPoints + earnedPoints);
            userRepository.save(user);
        }

        abandoned.setCompleted(true);
        abandonedRepository.save(abandoned);
    }

    public Long startGame(Long adventureId, String keycloakSub) {
        var user = userRepository.findByKeycloakSub(keycloakSub)
                .orElseThrow(() -> new RuntimeException("Felhasználó nem található"));

        var adventure = adventureRepository.findById(adventureId)
                .orElseThrow(() -> new RuntimeException("Kaland nem található"));

        var firstStation = stationRepository.findByAdventureIdAndSeqNumber(adventureId, 1)
                .orElseThrow(() -> new RuntimeException("Nincsenek állomások"));

        var abandoned = gameServiceMapper.buildAbandoned(user, adventure, firstStation.getId());
        return abandonedRepository.save(abandoned).getId();
    }

    @Mapper(config = AbstractMapper.class, imports = {LocalDateTime.class, LocalDate.class})
    public interface GameServiceMapper {

        @Mappings({
                @Mapping(target = "id", ignore = true),
                @Mapping(target = "user", source = "user"),
                @Mapping(target = "adventure", source = "adventure"),
                @Mapping(target = "lastStationId", source = "firstStationId"),
                @Mapping(target = "startedAt", expression = "java(LocalDateTime.now())"),
                @Mapping(target = "distanceTravelled", constant = "0.0"),
                @Mapping(target = "elapsedSec", constant = "0"),
                @Mapping(target = "points", constant = "0"),
                @Mapping(target = "completed", constant = "false")
        })
        AbandonedAdventureEntity buildAbandoned(UserEntity user, AdventureEntity adventure, Long firstStationId);

        @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
                nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS)
        @Mappings({
                @Mapping(target = "id", ignore = true),
                @Mapping(target = "user", ignore = true),
                @Mapping(target = "adventure", ignore = true),
                @Mapping(target = "startedAt", ignore = true),
                @Mapping(target = "distanceTravelled", source = "distanceInMeters"),
                @Mapping(target = "points", source = "points"),
                @Mapping(target = "completed", ignore = true)
        })
        void mergeUpdate(@MappingTarget AbandonedAdventureEntity target, ActiveGameDTO src);

        @Mappings({
                @Mapping(target = "id", ignore = true),
                @Mapping(target = "durationSec", source = "elapsedSec"),
                @Mapping(target = "points", source = "points"),
                @Mapping(target = "completedAt", expression = "java(LocalDate.now())")
        })
        CompletedAdventureEntity toCompleted(AbandonedAdventureEntity abandoned);
    }
}