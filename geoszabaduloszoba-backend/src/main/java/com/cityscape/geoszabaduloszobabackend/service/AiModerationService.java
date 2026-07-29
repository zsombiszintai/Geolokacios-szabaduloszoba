package com.cityscape.geoszabaduloszobabackend.service;

import com.cityscape.geoszabaduloszobabackend.model.dto.AiModerationResponse;
import com.cityscape.geoszabaduloszobabackend.model.entity.AdventureEntity;
import com.cityscape.geoszabaduloszobabackend.model.entity.StationEntity;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiModerationService {

    private final ChatModel chatModel;
    private final ObjectMapper objectMapper;

    public boolean checkStationDistances(List<StationEntity> stations, double maxDistanceKm) {
        if (stations == null || stations.size() < 2) {
            return true;
        }
        List<StationEntity> sortedStations = stations.stream()
                .sorted(Comparator.comparingInt(StationEntity::getSeqNumber))
                .toList();

        for (int i = 0; i < sortedStations.size() - 1; i++) {
            StationEntity s1 = sortedStations.get(i);
            StationEntity s2 = sortedStations.get(i + 1);

            if (s1.getLatitude() == null || s1.getLongitude() == null ||
                    s2.getLatitude() == null || s2.getLongitude() == null) {
                continue;
            }

            double distance = calculateHaversineDistance(
                    s1.getLatitude(), s1.getLongitude(),
                    s2.getLatitude(), s2.getLongitude()
            );

            if (distance > maxDistanceKm) {
                log.warn("Állomások közötti távolság túl nagy a(z) {}. és {}. állomás között: {} km (Max: {} km)",
                        s1.getSeqNumber(), s2.getSeqNumber(), distance, maxDistanceKm);
                return false;
            }
        }
        return true;
    }

    public AiModerationResponse evaluateTextWithAi(AdventureEntity adventure, List<StationEntity> stations) {
        String title = adventure.getTitle() != null ? adventure.getTitle() : "";
        String description = adventure.getDescription() != null ? adventure.getDescription() : "";

        StringBuilder stationsText = new StringBuilder();
        if (stations != null && !stations.isEmpty()) {
            List<StationEntity> sortedStations = stations.stream()
                    .sorted(Comparator.comparingInt(StationEntity::getSeqNumber))
                    .toList();

            for (StationEntity station : sortedStations) {
                stationsText.append(station.getSeqNumber())
                        .append(". Állomás: ")
                        .append(station.getContent())
                        .append("\n");
            }
        }

        String promptText = """
                Feladatod egy városi geokincskereső/szabadulószoba kaland szigorú automatikus elbírálása magyar nyelven.
                
                                Kaland címe: %s
                                Kaland leírása: %s
                
                                Állomások és tartalmuk:
                                %s
                
                                Értékelési szempontok és SZIGORÚ SZABÁLYOK:
                                1. isProfane: Van-e benne trágár, vulgáris vagy sértő kifejezés? (boolean: true/false)
                                2. profanityDetails: Ha isProfane = true, írd le a sértő kifejezést. Ha false, értéke legyen null.
                                3. isSolvable:\s
                                   - Kincskereső/szabadulószoba szempontból van-e valós, értelmes, megoldható feladvány vagy kincs leírás?
                                   - KRITIKUS SZABÁLY: Ha a cím, a leírás vagy bármelyik állomás tartalma halandzsa, tesztszöveg (pl. "asd", "123", értelmetlen betűhalmaz), túlságosan hiányos, vagy nem tartalmaz semmilyen érdemi leírást/feladványt, az isSolvable kötelezően FALSE!
                                4. solvabilityDetails: Ha isSolvable = false, írd le pontosan, miért lett elutasítva (pl. "A leírás vagy állomás csak tesztszöveget tartalmaz ('asd')."). Ha true, értéke legyen null.
                                5. overallApproved: KIZÁRÓLAG akkor true, ha isProfane = false ÉS isSolvable = true! Különben false.
                                6. reason: Ha overallApproved = false, írj egy 1-2 mondatos magyar nyelvű elutasítási indoklást a készítőnek, amiből megérti az elutasítás okát és tudja, hogy mit kell megváltoztatnia. Ha overallApproved = true, értéke legyen üres string ("").
                
                                KIZÁRÓLAG érvényes JSON formátumban válaszolj (más szöveg, markdown vagy magyarázat NÉLKÜL):
                                {
                                  "isProfane": false,
                                  "isSolvable": true,
                                  "profanityDetails": null,
                                  "solvabilityDetails": null,
                                  "overallApproved": true,
                                  "reason": ""
                                }
                """.formatted(title, description, stationsText.toString());

        try {
            String rawResponse = chatModel.call(promptText);

            String cleanedResponse = rawResponse.replaceAll("```json", "").replaceAll("```", "").trim();

            return objectMapper.readValue(cleanedResponse, AiModerationResponse.class);
        } catch (Exception e) {
            log.error("Hiba történt a JSON válasz feldolgozásakor az AI-tól", e);
            AiModerationResponse fallback = new AiModerationResponse();
            fallback.setProfane(true);
            fallback.setSolvable(false);
            fallback.setProfanityDetails("Automatikus ellenőrzési hiba.");
            fallback.setSolvabilityDetails("Nem értékelhető.");
            fallback.setOverallApproved(false);
            fallback.setReason("Rendszerhiba történt a kaland automatikus ellenőrzése során.");
            return fallback;
        }
    }

    private double calculateHaversineDistance(double lat1, double lon1, double lat2, double lon2) {
        final int EARTH_RADIUS_KM = 6371;

        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                        Math.sin(dLon / 2) * Math.sin(dLon / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return EARTH_RADIUS_KM * c;
    }
}