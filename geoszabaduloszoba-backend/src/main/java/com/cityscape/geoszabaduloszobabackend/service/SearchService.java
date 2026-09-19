package com.cityscape.geoszabaduloszobabackend.service;

import com.cityscape.geoszabaduloszobabackend.model.dto.SearchDTO;
import com.cityscape.geoszabaduloszobabackend.model.entity.UserEntity;
import com.cityscape.geoszabaduloszobabackend.repository.ListRepository;
import com.cityscape.geoszabaduloszobabackend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SearchService {

    private final AdventureService adventureService;
    private final UserRepository userRepository;
    private final ListRepository listRepository;
    private final KeycloakAdminService keycloakAdminService;

    public List<SearchDTO> searchEverything(
            String query,
            String type,
            Double lat,
            Double lon
    ) {
        if (query == null || query.isBlank()
                || type == null || type.isBlank()) {
            return List.of();
        }

        String term = query.trim().toLowerCase(Locale.ROOT);

        return switch (type.trim().toUpperCase(Locale.ROOT)) {
            case "USER" -> searchUsers(term);

            case "ADVENTURE" -> adventureService
                    .searchAndMap(term, lat, lon)
                    .stream()
                    .map(adv -> SearchDTO.builder()
                            .id(adv.id())
                            .title(adv.title())
                            .lat(adv.advLat())
                            .lon(adv.advLon())
                            .type("ADVENTURE")
                            .build())
                    .toList();

            case "LIST" -> listRepository
                    .findByTitleContainingIgnoreCase(term)
                    .stream()
                    .map(list -> SearchDTO.builder()
                            .id(list.getId())
                            .title(list.getTitle())
                            .type("LIST")
                            .build())
                    .toList();

            default -> List.of();
        };
    }

    private List<SearchDTO> searchUsers(String term) {
        var keycloakUsers = keycloakAdminService.searchByUsername(term);

        if (keycloakUsers.isEmpty()) {
            return List.of();
        }

        List<String> keycloakSubs = keycloakUsers.stream()
                .map(user -> user.getId())
                .toList();

        Map<String, Long> localIdsBySub = userRepository
                .findByKeycloakSubIn(keycloakSubs)
                .stream()
                .collect(Collectors.toMap(
                        UserEntity::getKeycloakSub,
                        UserEntity::getId
                ));

        return keycloakUsers.stream()
                .filter(user -> localIdsBySub.containsKey(user.getId()))
                .map(user -> SearchDTO.builder()
                        .id(localIdsBySub.get(user.getId()))
                        .title(user.getUsername())
                        .type("USER")
                        .build())
                .toList();
    }
}