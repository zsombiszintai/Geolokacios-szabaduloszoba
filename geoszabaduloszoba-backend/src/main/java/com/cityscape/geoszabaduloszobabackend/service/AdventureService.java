package com.cityscape.geoszabaduloszobabackend.service;

import com.cityscape.geoszabaduloszobabackend.model.dto.*;
import com.cityscape.geoszabaduloszobabackend.model.dto.AdventureProfileDTO;
import com.cityscape.geoszabaduloszobabackend.model.entity.AdventureEntity;
import com.cityscape.geoszabaduloszobabackend.model.entity.StationEntity;
import com.cityscape.geoszabaduloszobabackend.model.entity.UserEntity;
import com.cityscape.geoszabaduloszobabackend.repository.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

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
    private final ObjectMapper objectMapper;
    private final KeycloakAdminService keycloakAdminService;

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

    @Transactional(readOnly = true)
    public List<NearbyAdventureDTO> searchAndMap(
            String query,
            Double uLat,
            Double uLon
    ) {
        final String PUBLIC_STATUS = "PUBLIC";
        final boolean hasUserLocation = uLat != null && uLon != null;

        List<AdventureEntity> adventures;

        if (query == null || query.isBlank()) {
            adventures = adventureRepository.findByStatus(PUBLIC_STATUS);
        } else {
            adventures = adventureRepository
                    .findByTitleContainingIgnoreCase(query.trim());
        }

        return adventures.stream()
                .filter(adv -> PUBLIC_STATUS.equals(adv.getStatus()))
                .map(adv -> {
                    Optional<StationEntity> startStation =
                            stationRepository.findByAdventureIdAndSeqNumber(
                                    adv.getId(), 0
                            ).or(() ->
                                    stationRepository.findByAdventureIdAndSeqNumber(
                                            adv.getId(), 1
                                    )
                            );

                    if (startStation.isEmpty()) {
                        return null;
                    }

                    StationEntity station = startStation.get();
                    Double advLat = station.getLatitude();
                    Double advLon = station.getLongitude();

                    if (advLat == null || advLon == null) {
                        return null;
                    }

                    Integer distanceInMeters = null;

                    if (hasUserLocation) {
                        distanceInMeters = calculateDistance(
                                uLat, uLon, advLat, advLon
                        );
                    }

                    return new NearbyAdventureDTO(
                            adv.getId(),
                            adv.getTitle(),
                            distanceInMeters,
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
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Kaland nem található"
                ));

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
        dto.setCreatorName(resolveCreatorName(adv.getCreator()));;
        dto.setAverageRating(adv.getAverageRating() != null ? adv.getAverageRating() : 0.0);
        dto.setHasStartingPoint(containsStartingPoint(stationEntities));

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
    public AdventureEntity createAdventureWithStations(
            AdventureEntity adventure,
            List<StationEntity> stations
    ) {
        boolean draft = "DRAFT".equals(adventure.getStatus());

        List<StationEntity> preparedStations =
                prepareStations(stations, draft);

        UserEntity user = userService.getOrCreateCurrentUser();

        adventure.setCreator(user);
        adventure.setHasStartingPoint(
                containsStartingPoint(preparedStations)
        );
        adventure.setTotalDistance(
                calculateTotalDistance(preparedStations)
        );

        AdventureEntity savedAdventure =
                adventureRepository.save(adventure);

        if (!preparedStations.isEmpty()) {
            stationService.saveStations(preparedStations, savedAdventure);
        }

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

    public AdventureCreateDTO getAdventureForEdit(Long id) {

        UserEntity currentUser = userService.getOrCreateCurrentUser();

        AdventureEntity adv = adventureRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Kaland nem található"));

        if (!adv.getCreator().getId().equals(currentUser.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Nincs jogosultságod a kaland szerkesztéséhez!");
        }

        List<StationEntity> stations = stationRepository.findAllByAdventureIdOrderBySeqNumberAsc(id);

        AdventureCreateDTO dto = new AdventureCreateDTO();
        dto.setTitle(adv.getTitle());
        dto.setDescription(adv.getDescription());
        dto.setDifficulty(adv.getDifficulty() != null ? adv.getDifficulty().name() : "EASY");
        dto.setStatus(adv.getStatus());

        int lastPlayableSequence = stations.stream()
                .map(StationEntity::getSeqNumber)
                .filter(Objects::nonNull)
                .filter(sequence -> sequence > 0)
                .max(Integer::compareTo)
                .orElse(-1);

        dto.setHasStartingPoint(containsStartingPoint(stations));

        List<StationCreateDTO> stationDTOs = stations.stream().map(s -> {
            StationContent content = null;
            if (s.getContent() != null && !s.getContent().isBlank()) {
                try {
                    content = objectMapper.readValue(s.getContent(), StationContent.class);
                } catch (Exception e) {
                    throw new RuntimeException("Hiba a StationContent JSON-nal");
                }
            }

            boolean isLast = s.getSeqNumber() != null
                    && s.getSeqNumber() > 0
                    && s.getSeqNumber() == lastPlayableSequence;

            return new StationCreateDTO(
                    s.getSeqNumber(),
                    content,
                    s.getLatitude(),
                    s.getLongitude(),
                    isLast
            );
        }).toList();

        dto.setStations(stationDTOs);
        return dto;
    }

    @Transactional
    public AdventureEntity updateAdventureWithStations(
            Long id,
            AdventureEntity updatedAdventure,
            List<StationEntity> newStations
    ) {
        UserEntity currentUser = userService.getOrCreateCurrentUser();

        AdventureEntity existing = adventureRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Kaland nem található."
                ));

        if (!existing.getCreator().getId().equals(currentUser.getId())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Nincs jogosultságod a kaland módosításához!"
            );
        }

        String status = updatedAdventure.getStatus() != null
                ? updatedAdventure.getStatus()
                : existing.getStatus();

        List<StationEntity> preparedStations =
                prepareStations(newStations, "DRAFT".equals(status));

        existing.setTitle(updatedAdventure.getTitle());
        existing.setDescription(updatedAdventure.getDescription());
        existing.setDifficulty(updatedAdventure.getDifficulty());
        existing.setStatus(status);

        existing.setHasStartingPoint(
                containsStartingPoint(preparedStations)
        );
        existing.setTotalDistance(
                calculateTotalDistance(preparedStations)
        );

        stationRepository.deleteByAdventureId(id);
        stationRepository.flush();

        AdventureEntity savedAdventure =
                adventureRepository.save(existing);

        if (!preparedStations.isEmpty()) {
            stationService.saveStations(preparedStations, savedAdventure);
        }

        return savedAdventure;
    }

    /// SEGÉD METÓDUSOK

    private List<StationEntity> prepareStations(
            List<StationEntity> stations,
            boolean draft
    ) {
        if (stations == null || stations.isEmpty()) {
            if (draft) {
                return List.of();
            }

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Legalább egy rejtvényes állomás szükséges."
            );
        }

        for (StationEntity station : stations) {
            if (station == null
                    || station.getSeqNumber() == null
                    || station.getSeqNumber() < 0) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Hiányzó vagy hibás állomássorszám."
                );
            }
        }

        List<StationEntity> sorted = stations.stream()
                .sorted(Comparator.comparingInt(StationEntity::getSeqNumber))
                .toList();

        int firstSequence = sorted.get(0).getSeqNumber();

        if (firstSequence != 0 && firstSequence != 1) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Az állomások számozása 0-val vagy 1-gyel kezdődjön."
            );
        }

        for (int i = 0; i < sorted.size(); i++) {
            StationEntity station = sorted.get(i);

            if (station.getSeqNumber() != firstSequence + i) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Az állomások sorszámai legyenek folytonosak és egyediek."
                );
            }

            Double latitude = station.getLatitude();
            Double longitude = station.getLongitude();

            // Piszkozatban a rejtvényes állomás helye még hiányozhat.
            boolean missingLocationAllowed =
                    draft && station.getSeqNumber() > 0;

            if (latitude == null || longitude == null) {
                if (!missingLocationAllowed
                        || latitude != null
                        || longitude != null) {
                    throw new ResponseStatusException(
                            HttpStatus.BAD_REQUEST,
                            "Az állomás mindkét koordinátáját meg kell adni."
                    );
                }
            } else if (!Double.isFinite(latitude)
                    || !Double.isFinite(longitude)
                    || latitude < -90 || latitude > 90
                    || longitude < -180 || longitude > 180) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Érvénytelen állomáskoordináták."
                );
            }

            station.setLastStation(false);
        }

        StationEntity last = sorted.get(sorted.size() - 1);

        if (last.getSeqNumber() > 0) {
            last.setLastStation(true);
        } else if (!draft) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "A kezdőpont mellé legalább egy rejtvényes állomás szükséges."
            );
        }

        return sorted;
    }

    private boolean containsStartingPoint(List<StationEntity> stations) {
        return stations.stream()
                .anyMatch(station ->
                        Integer.valueOf(0).equals(station.getSeqNumber())
                );
    }

    private double calculateTotalDistance(List<StationEntity> stations) {
        double totalDistance = 0.0;

        for (int i = 0; i < stations.size() - 1; i++) {
            StationEntity current = stations.get(i);
            StationEntity next = stations.get(i + 1);

            if (current.getLatitude() == null
                    || current.getLongitude() == null
                    || next.getLatitude() == null
                    || next.getLongitude() == null) {
                continue;
            }

            totalDistance += calculateDistance(
                    current.getLatitude(),
                    current.getLongitude(),
                    next.getLatitude(),
                    next.getLongitude()
            );
        }

        return totalDistance;
    }

    private String resolveCreatorName(UserEntity creator) {
        if (creator == null
                || creator.getKeycloakSub() == null
                || creator.getKeycloakSub().isBlank()) {
            return "Ismeretlen";
        }

        try {
            String username = keycloakAdminService.getUsername(
                    creator.getKeycloakSub()
            );

            return username == null || username.isBlank()
                    ? "Ismeretlen"
                    : username;

        } catch (ResponseStatusException exception) {
            if (exception.getStatusCode().value() == 404) {
                return "Ismeretlen";
            }

            throw exception;
        }
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