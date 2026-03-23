package com.cityscape.geoszabaduloszobabackend.service;

import com.cityscape.geoszabaduloszobabackend.model.dto.AdventureDTO;
import com.cityscape.geoszabaduloszobabackend.model.dto.NearbyAdventureDTO;
import com.cityscape.geoszabaduloszobabackend.model.entity.AdventureEntity;
import com.cityscape.geoszabaduloszobabackend.model.entity.StationEntity;
import com.cityscape.geoszabaduloszobabackend.repository.AdventureRepository;
import com.cityscape.geoszabaduloszobabackend.repository.StationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AdventureService{

    private final AdventureRepository adventureRepository;
    private final StationRepository stationRepository;

    public List<NearbyAdventureDTO> searchAndMap(String query, Double uLat, Double uLon) {

        List<AdventureEntity> adventures;

        if (query == null || query.isBlank()) {

            adventures = adventureRepository.findAll();

        } else {
            adventures = adventureRepository.findByTitleContainingIgnoreCase(query);
        }

        return adventures.stream()
                .map(adv -> {
                    Optional<StationEntity> startStation = stationRepository.findByAdventureIdAndSeqNumber(adv.getId(), 1);

                    Double advLat = startStation.map(StationEntity::getLatitude).orElse(0.0);
                    Double advLon = startStation.map(StationEntity::getLongitude).orElse(0.0);

                    return new NearbyAdventureDTO(

                            adv.getId(),
                            adv.getTitle(),
                            calculateDistance(uLat, uLon, advLat, advLon),
                            formatTime(adv.getAverageTimeInSeconds())

                    );
                })
                .toList();
    }


    public AdventureDTO getDetails(Long id, Double uLat, Double uLon) {

        AdventureEntity adv = adventureRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Kaland nem található"));

        Optional<StationEntity> startStation = stationRepository.findByAdventureIdAndSeqNumber(adv.getId(), 1);
        Double advLat = startStation.map(StationEntity::getLatitude).orElse(0.0);
        Double advLon = startStation.map(StationEntity::getLongitude).orElse(0.0);

        AdventureDTO dto = new AdventureDTO();
        dto.setId(adv.getId());
        dto.setTitle(adv.getTitle());
        dto.setAverageTime(formatTime(adv.getAverageTimeInSeconds()));
        dto.setDistanceInMeters(calculateDistance(uLat, uLon, advLat, advLon));

        dto.setDifficulty(translateDifficulty(adv.getDifficulty()));

        dto.setCreatorName(adv.getCreator() != null ? adv.getCreator().getUsername() : "Ismeretlen");
        dto.setAverageRating(adv.getAverageRating() != null ? adv.getAverageRating() : 0.0);

        dto.setRatingDistribution(List.of(5, 10, 45, 30, 10));

        return dto;
    }

    

    /// SEGÉD METÓDUSOK
    private String translateDifficulty(Integer diff) {
        if (diff == null) return "Ismeretlen";
        return switch (diff) {
            case 1 -> "Könnyű";
            case 2 -> "Közepes";
            case 3 -> "Nehéz";
            default -> "Ismeretlen";
        };
    }


    private Integer calculateDistance(double lat1, double lon1, double lat2, double lon2) {

        double R = 6371e3;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                        Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return (int) (R * c);
    }



    private String formatTime(Integer totalSeconds) {

        if (totalSeconds == null || totalSeconds == 0) return "0 m";

        long totalMinutes = totalSeconds / 60;
        long hours = totalMinutes / 60;
        long minutes = totalMinutes % 60;

        if (hours > 0) {
            return hours + " h " + minutes + " m";
        } else {
            return minutes + " m";
        }

    }

}