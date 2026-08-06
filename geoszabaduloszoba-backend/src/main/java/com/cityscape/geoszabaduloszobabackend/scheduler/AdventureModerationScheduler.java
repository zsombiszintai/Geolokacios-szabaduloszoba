package com.cityscape.geoszabaduloszobabackend.scheduler;

import com.cityscape.geoszabaduloszobabackend.model.dto.AiModerationResponse;
import com.cityscape.geoszabaduloszobabackend.model.entity.AdventureEntity;
import com.cityscape.geoszabaduloszobabackend.model.entity.ModerationResponseEntity;
import com.cityscape.geoszabaduloszobabackend.model.entity.StationEntity;
import com.cityscape.geoszabaduloszobabackend.repository.AdventureRepository;
import com.cityscape.geoszabaduloszobabackend.repository.ModerationResponseRepository;
import com.cityscape.geoszabaduloszobabackend.repository.StationRepository;
import com.cityscape.geoszabaduloszobabackend.service.AiModerationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class AdventureModerationScheduler {

    private final AdventureRepository adventureRepository;
    private final StationRepository stationRepository;
    private final AiModerationService moderationService;
    private final ModerationResponseRepository moderationResponseRepository;

    private static final double MAX_STATION_DISTANCE_KM = 5.0;

    @Scheduled(fixedDelay = 50000)
    @Transactional
    public void processPendingAdventures() {
        List<AdventureEntity> pendingList = adventureRepository.findTop3ByStatusOrderByIdAsc("PENDING");

        if (pendingList.isEmpty()) {
            return;
        }

        log.info("{} db elbírálásra váró kaland feldolgozása kezdődik...", pendingList.size());

        for (AdventureEntity adventure : pendingList) {
            try {
                List<StationEntity> stations = stationRepository.findByAdventureIdOrderBySeqNumberAsc(adventure.getId());

                boolean distanceOk = moderationService.checkStationDistances(stations, MAX_STATION_DISTANCE_KM);
                if (!distanceOk) {
                    adventure.setStatus("REJECTED");
                    adventureRepository.save(adventure);
                    log.warn("Kaland (ID: {}) elutasítva a túl nagy távolságok miatt.", adventure.getId());
                    continue;
                }

                AiModerationResponse aiResponse = moderationService.evaluateTextWithAi(adventure, stations);

                if (!aiResponse.isOverallApproved()) {
                    adventure.setStatus("REJECTED");

                    String reason = (aiResponse.getReason() != null && !aiResponse.getReason().isBlank())
                            ? aiResponse.getReason()
                            : "A kaland nem felelt meg a moderációs elveknek.";

                    saveModerationReason(adventure, reason);
                    log.info("Kaland (ID: {}) elutasítva AI által. Indok: {} (Trágár details: {}, Megfejthetőség details: {})",
                            adventure.getId(), reason, aiResponse.getProfanityDetails(), aiResponse.getSolvabilityDetails());
                } else {
                    adventure.setStatus("PUBLIC");
                    log.info("Kaland (ID: {}) elfogadva!", adventure.getId());
                }

                adventureRepository.save(adventure);

                Thread.sleep(1000);

            } catch (Exception e) {
                log.error("Hiba történt a(z) {} ID-jú kaland elbírálásakor: ", adventure.getId(), e);
            }
        }
    }

    private void saveModerationReason(AdventureEntity adventure, String reason) {
        ModerationResponseEntity responseEntity = new ModerationResponseEntity();
        responseEntity.setAdventure(adventure);
        responseEntity.setReason(reason);
        responseEntity.setCreatedAt(LocalDateTime.now());
        moderationResponseRepository.save(responseEntity);
    }
}