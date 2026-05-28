package com.cityscape.geoszabaduloszobabackend.service;

import com.cityscape.geoszabaduloszobabackend.model.dto.*;
import com.cityscape.geoszabaduloszobabackend.model.dto.AdventureProfileDTO;
import com.cityscape.geoszabaduloszobabackend.model.entity.AdventureEntity;
import com.cityscape.geoszabaduloszobabackend.model.entity.StationEntity;
import com.cityscape.geoszabaduloszobabackend.model.entity.UserEntity;
import com.cityscape.geoszabaduloszobabackend.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AdventureService{

    private final AdventureRepository adventureRepository;
    private final StationRepository stationRepository;
    private final UserService userService;
    private final StationService stationService;
    private final AbandonedAdventureRepository abandonedRepository;
    private final ReviewRepository reviewRepository;

    public List<AbandonedAdventureDTO> getAllAbandonedByUser(String sub) {

        return abandonedRepository.findAllByUserKeycloakSub(sub).stream()
                .filter(entity -> !entity.isCompleted())
                .map(entity -> {
                    Integer seqNum = stationRepository.findById(entity.getLastStationId())
                            .map(StationEntity::getSeqNumber)
                            .orElse(1);

                    return new AbandonedAdventureDTO(
                            entity.getAdventure().getId(),
                            entity.getAdventure().getTitle(),
                            entity.getLastStationId(),
                            seqNum,
                            entity.getElapsedSec(),
                            entity.getDistanceTravelled(),
                            entity.getPoints()
                    );
                })
                .toList();
    }

    public List<AdventureCreatedDTO> getAdventuresByUser(UserEntity creator) {
        return adventureRepository.findAllByCreator(creator).stream()
                .map(entity -> new AdventureCreatedDTO(
                        entity.getId(),
                        entity.getTitle(),
                        entity.getCreatedAt(),
                        entity.getStatus()
                ))
                .toList();
    }

    public List<NearbyAdventureDTO> searchAndMap(String query, Double uLat, Double uLon) {

        List<AdventureEntity> adventures;
        final String PUBLIC_STATUS = "PUBLIC";

        if (query == null || query.isBlank()) {
            adventures = adventureRepository.findByStatus(PUBLIC_STATUS);
        } else {
            adventures = adventureRepository.findByTitleContainingIgnoreCase(query);
        }

        return adventures.stream()
                .map(adv -> {
                    Optional<StationEntity> startStation = stationRepository.findByAdventureIdAndSeqNumber(adv.getId(), 1);

                    if (startStation.isEmpty()) return null;

                    Double advLat = startStation.map(StationEntity::getLatitude).orElse(0.0);
                    Double advLon = startStation.map(StationEntity::getLongitude).orElse(0.0);

                    return new NearbyAdventureDTO(

                            adv.getId(),
                            adv.getTitle(),
                            calculateDistance(uLat, uLon, advLat, advLon),
                            adv.getAverageTimeInSeconds(),
                            advLon,
                            advLat

                    );
                })
                .filter(Objects::nonNull)
                .toList();
    }


    public AdventureProfileDTO getDetails(Long id, Double uLat, Double uLon) {

        AdventureEntity adv = adventureRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Kaland nem található"));

        List<StationEntity> stationEntities = stationRepository.findAllByAdventureIdOrderBySeqNumberAsc(id);

        List<StationDTO> stationDTOs = stationEntities.stream()
                .map(stationService::convertToDTO)
                .toList();

        AdventureProfileDTO dto = new AdventureProfileDTO();
        dto.setId(adv.getId());
        dto.setTitle(adv.getTitle());
        dto.setDescription(adv.getDescription());
        dto.setAverageTime(formatTime(adv.getAverageTimeInSeconds()));
        dto.setDistanceInMeters(adv.getTotalDistance());
        dto.setDifficulty(adv.getDifficulty() != null ? adv.getDifficulty().getDisplayName() : "Ismeretlen");
        dto.setCreatorName(adv.getCreator() != null ? adv.getCreator().getUsername() : "Ismeretlen");
        dto.setAverageRating(adv.getAverageRating() != null ? adv.getAverageRating() : 0.0);

        List<ReviewDTO> reviews = reviewRepository.findByAdventureId(id).stream()
                .map(r -> new ReviewDTO(
                        r.getId(),
                        r.getAdventure().getId(),
                        r.getAdventure().getTitle(),
                        r.getRating(),
                        r.getReviewText(),
                        r.getReviewedAt()
                )).toList();

        dto.setReviews(reviews);

        dto.setStations(stationDTOs);

        return dto;
    }

    @Transactional
    public AdventureEntity createAdventureWithStations(AdventureEntity adventure, List<StationEntity> stations) {

        UserEntity user = userService.getOrCreateCurrentUser();
        adventure.setCreator(user);

        double totalDistance = 0.0;
        if (stations != null && stations.size() > 1) {

            for (int i = 0; i < stations.size(); i++) {
                stations.get(i).setSeqNumber(i + 1);
            }

            List<StationEntity> sortedStations = stations.stream()
                    .sorted(Comparator.comparingInt(StationEntity::getSeqNumber))
                    .toList();

            for (int i = 0; i < sortedStations.size() - 1; i++) {
                StationEntity current = sortedStations.get(i);
                StationEntity next = sortedStations.get(i + 1);

                if (current.getLatitude() != null && current.getLongitude() != null &&
                        next.getLatitude() != null && next.getLongitude() != null) {

                    totalDistance += calculateDistance(
                            current.getLatitude(), current.getLongitude(),
                            next.getLatitude(), next.getLongitude()
                    );
                }
            }
        }
        adventure.setTotalDistance(totalDistance);

        AdventureEntity savedAdventure = adventureRepository.save(adventure);
        stationService.saveStations(stations, savedAdventure);

        return savedAdventure;
    }

    @Transactional
    public void deleteAdventure(Long id) {
        if (!adventureRepository.existsById(id)) {
            throw new RuntimeException("A kaland nem található ezzel az azonosítóval: " + id);
        }

        stationRepository.deleteByAdventureId(id);

        adventureRepository.deleteById(id);
    }

    /// SEGÉD METÓDUSOK

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
        if (totalSeconds == null || totalSeconds == 0) return "0 s";

        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;

        StringBuilder sb = new StringBuilder();

        if (hours > 0) {
            sb.append(hours).append(" h ");
        }

        if (minutes > 0 || hours > 0) {
            sb.append(minutes).append(" m ");
        }

        if (seconds > 0 || (hours == 0 && minutes == 0)) {
            sb.append(seconds).append(" s");
        }

        return sb.toString().trim();
    }


}