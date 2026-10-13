package com.cityscape.geoszabaduloszobabackend.service;

import com.cityscape.geoszabaduloszobabackend.model.domain.SearchResult;
import com.cityscape.geoszabaduloszobabackend.model.entity.UserEntity;
import com.cityscape.geoszabaduloszobabackend.repository.ListRepository;
import com.cityscape.geoszabaduloszobabackend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.Page;
import org.springframework.transaction.annotation.Transactional;

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

    public List<SearchResult> searchEverything(
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
                    .searchNearby(term, lat, lon)
                    .stream()
                    .map(adv -> SearchResult.builder()
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
                    .map(list -> SearchResult.builder()
                            .id(list.getId())
                            .title(list.getTitle())
                            .type("LIST")
                            .build())
                    .toList();

            default -> List.of();
        };
    }


    @Transactional(readOnly = true)
    public Page<SearchResult> searchPage(String query, String type, Double lat, Double lon, int page, int size) {
        var pageable = SearchPagination.request(page, size);
        if (query == null || query.isBlank() || type == null || type.isBlank()) return Page.empty(pageable);
        String term = query.trim().toLowerCase(Locale.ROOT);
        return switch (type.trim().toUpperCase(Locale.ROOT)) {
            case "ADVENTURE" -> adventureService.searchNearbyPage(term, lat, lon, page, size)
                    .map(adventure -> SearchResult.builder().id(adventure.id()).title(adventure.title())
                            .lat(adventure.advLat()).lon(adventure.advLon()).type("ADVENTURE").build());
            case "LIST" -> listRepository.findByTitleContainingIgnoreCase(term, pageable)
                    .map(list -> SearchResult.builder().id(list.getId()).title(list.getTitle()).type("LIST").build());
            case "USER" -> {
                // Preserve the existing Keycloak search limit and local-profile eligibility.
                var users = keycloakAdminService.searchByUsername(term);
                if (users.isEmpty()) yield Page.empty(pageable);
                var usernames = users.stream().collect(Collectors.toMap(user -> user.getId(), user -> user.getUsername()));
                yield userRepository.findByKeycloakSubIn(List.copyOf(usernames.keySet()), pageable)
                        .map(user -> SearchResult.builder().id(user.getId())
                                .title(usernames.get(user.getKeycloakSub())).type("USER").build());
            }
            default -> Page.empty(pageable);
        };
    }

    private List<SearchResult> searchUsers(String term) {
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
                .map(user -> SearchResult.builder()
                        .id(localIdsBySub.get(user.getId()))
                        .title(user.getUsername())
                        .type("USER")
                        .build())
                .toList();
    }
}