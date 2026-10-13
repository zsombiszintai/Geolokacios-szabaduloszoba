package com.cityscape.geoszabaduloszobabackend.service;

import com.cityscape.geoszabaduloszobabackend.model.domain.*;
import com.cityscape.geoszabaduloszobabackend.model.entity.AdventureEntity;
import com.cityscape.geoszabaduloszobabackend.model.entity.AiModerationResponseEntity;
import com.cityscape.geoszabaduloszobabackend.model.entity.StationEntity;
import com.cityscape.geoszabaduloszobabackend.model.entity.UserEntity;
import com.cityscape.geoszabaduloszobabackend.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.Page;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdventureService{

    private final AdventureRepository adventureRepository;
    private final StationRepository stationRepository;
    private final UserService userService;
    private final StationService stationService;
    private final AbandonedAdventureRepository abandonedRepository;
    private final ReviewRepository reviewRepository;
    private final KeycloakAdminService keycloakAdminService;
    private final AvatarStorageService avatarStorageService;
    private final AiModerationResponseRepository moderationRepository;


    @Transactional
    public void saveModeration(
            Long adventureId,
            ModerationResult result
    ) {
        AdventureEntity adventure = adventureRepository.findById(adventureId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Kaland nem található."
                ));

        AiModerationResponseEntity entity =
                new AiModerationResponseEntity();

        entity.setAdventure(adventure);
        entity.setProfane(result.isProfane());
        entity.setSolvable(result.isSolvable());
        entity.setProfanityDetails(result.getProfanityDetails());
        entity.setSolvabilityDetails(result.getSolvabilityDetails());
        entity.setOverallApproved(result.isOverallApproved());
        entity.setReason(result.getReason());

        moderationRepository.save(entity);
    }

    public List<AbandonedAdventure> getAllAbandonedByUser() {
        String sub = userService.getCurrentUserId();
        return abandonedRepository.findAllByUserKeycloakSub(sub).stream()
                .filter(session -> !session.isCompleted())
                .map(session -> new AbandonedAdventure(session,
                        stationRepository.findById(session.getLastStationId())
                                .map(StationEntity::getSeqNumber).orElse(1)))
                .toList();
    }

    @Transactional
    public List<CreatedAdventure> getMyAdventures() {
        UserEntity creator = userService.getOrCreateCurrentUser();
        List<AdventureEntity> adventures = adventureRepository.findAllByCreator(creator);
        if (adventures.isEmpty()) return List.of();
        List<Long> ids = adventures.stream().map(AdventureEntity::getId).toList();
        Map<Long, AiModerationResponseEntity> moderationByAdventure = moderationRepository
                .findLatestByAdventureIds(ids).stream()
                .collect(Collectors.toMap(value -> value.getAdventure().getId(), value -> value));
        return adventures.stream()
                .map(adventure -> new CreatedAdventure(adventure, moderationByAdventure.get(adventure.getId())))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<NearbyAdventureResult> searchNearby(
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

                    return new NearbyAdventureResult(
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


    public AdventureDetails getDetails(Long id, Double uLat, Double uLon) {
        AdventureEntity adventure = adventureRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Kaland nem található"));
        UserEntity creator = adventure.getCreator();
        return new AdventureDetails(adventure,
                stationRepository.findAllByAdventureIdOrderBySeqNumberAsc(id),
                reviewRepository.findByAdventureId(id), resolveCreatorName(creator),
                formatAvatarUrl(creator != null ? creator.getProfilePictureUrl() : null));
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
        moderationRepository.deleteByAdventureId(id);
        moderationRepository.flush();
        adventureRepository.deleteById(id);
    }

    public AdventureEdit getAdventureForEdit(Long id) {
        UserEntity currentUser = userService.getOrCreateCurrentUser();
        AdventureEntity adventure = adventureRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Kaland nem található"));
        if (!adventure.getCreator().getId().equals(currentUser.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Nincs jogosultságod a kaland szerkesztéséhez!");
        }
        return new AdventureEdit(adventure, stationRepository.findAllByAdventureIdOrderBySeqNumberAsc(id));
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


    @Transactional(readOnly = true)
    public Page<NearbyAdventureResult> searchNearbyPage(String query, Double lat, Double lon, int page, int size) {
        var pageable = SearchPagination.request(page, size);
        String term = query == null ? "" : query.trim();
        // Match Spring Data's literal substring semantics for '%' and '_'.
        term = term.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
        return adventureRepository.searchPublicWithLocation(term, pageable).map(adventure -> {
            StationEntity station = stationRepository.findByAdventureIdAndSeqNumber(adventure.getId(), 0)
                    .or(() -> stationRepository.findByAdventureIdAndSeqNumber(adventure.getId(), 1))
                    .orElseThrow();
            Integer distance = lat != null && lon != null
                    ? calculateDistance(lat, lon, station.getLatitude(), station.getLongitude()) : null;
            return new NearbyAdventureResult(adventure.getId(), adventure.getTitle(), distance,
                    adventure.getAverageTimeInSeconds(), station.getLongitude(), station.getLatitude());
        });
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

    private String formatAvatarUrl(String urlOrKey) {
        if (urlOrKey == null
                || urlOrKey.isBlank()
                || "/images/default-avatar.png".equals(urlOrKey)
                || "images/default-avatar.png".equals(urlOrKey)) {
            return null;
        }

        if (urlOrKey.startsWith("https://")
                || urlOrKey.startsWith("http://")) {
            return urlOrKey;
        }

        return avatarStorageService.publicUrl(urlOrKey);
    }

}