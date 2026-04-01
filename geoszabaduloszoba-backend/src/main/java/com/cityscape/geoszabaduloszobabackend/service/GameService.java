package com.cityscape.geoszabaduloszobabackend.service;

import com.cityscape.geoszabaduloszobabackend.model.dto.ActiveGameDTO;
import com.cityscape.geoszabaduloszobabackend.model.entity.*;
import com.cityscape.geoszabaduloszobabackend.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class GameService {
    private final AbandonedAdventureRepository abandonedRepository;
    private final CompletedAdventureRepository completedRepository;
    private final StationRepository stationRepository;
    private final UserRepository userRepository;
    private final AdventureRepository adventureRepository;

    @Transactional
    public void updateActiveGame(ActiveGameDTO dto) {
        AbandonedAdventureEntity session = abandonedRepository.findById(dto.sessionId())
                .orElseThrow(() -> new RuntimeException("Session nem található"));

        System.out.println("Mentés: SessionID: " + dto.sessionId() + " Távolság: " + dto.distanceInMeters());

        session.setElapsedSec(dto.elapsedSec());
        session.setDistanceTravelled(dto.distanceInMeters());
        session.setLastStationId(dto.lastStationId());

        abandonedRepository.saveAndFlush(session);

        stationRepository.findById(dto.lastStationId()).ifPresent(currentStation -> {
            if (currentStation.isLastStation()) {
                finishGame(session);
            }
        });
    }

    private void finishGame(AbandonedAdventureEntity abandoned) {
        CompletedAdventureEntity completed = new CompletedAdventureEntity();

        completed.setUser(abandoned.getUser());
        completed.setAdventure(abandoned.getAdventure());
        completed.setDurationSec(abandoned.getElapsedSec());
        completed.setDistanceTravelled(abandoned.getDistanceTravelled());
        completed.setCompletedAt(LocalDate.now());

        completedRepository.save(completed);

        abandoned.setCompleted(true);
        abandonedRepository.save(abandoned);
    }

    @Transactional
    public Long startGame(Long adventureId, String KeycloakSub) {
        UserEntity user = userRepository.findByKeycloakSub(KeycloakSub)
                .orElseThrow(() -> new RuntimeException("Felhasználó nem található: " + KeycloakSub));

        AdventureEntity adventure = adventureRepository.findById(adventureId)
                .orElseThrow(() -> new RuntimeException("Kaland nem található: " + adventureId));

        StationEntity firstStation = stationRepository.findByAdventureIdAndSeqNumber(adventureId, 1)
                .orElseThrow(() -> new RuntimeException("Ehhez a kalandhoz nincsenek állomások!"));

        AbandonedAdventureEntity abandoned = new AbandonedAdventureEntity();
        abandoned.setUser(user);
        abandoned.setAdventure(adventure);
        abandoned.setLastStationId(firstStation.getId());
        abandoned.setStartedAt(LocalDateTime.now());
        abandoned.setElapsedSec(0);
        abandoned.setDistanceTravelled(0.0);
        abandoned.setCompleted(false);

        AbandonedAdventureEntity saved = abandonedRepository.save(abandoned);
        return saved.getId();
    }
}
