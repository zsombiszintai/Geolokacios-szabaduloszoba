package com.cityscape.geoszabaduloszobabackend.service;

import com.cityscape.geoszabaduloszobabackend.model.dto.SearchDTO;
import com.cityscape.geoszabaduloszobabackend.repository.ListRepository;
import com.cityscape.geoszabaduloszobabackend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SearchService {

    private final AdventureService adventureService;
    private final UserRepository  userRepository;
    private final ListRepository listRepository;

    public List<SearchDTO> searchEverything(String query, String type, Double lat, Double lon) {
        String term = query.toLowerCase();

        return switch (type.toUpperCase()) {
            case "USER" -> userRepository.findByUsernameContainingIgnoreCase(term)
                    .stream()
                    .map(user -> SearchDTO.builder()
                            .id(user.getId())
                            .title(user.getUsername())
                            .type("USER")
                            .build())
                    .toList();

            case "ADVENTURE" -> adventureService.searchAndMap(term, lat, lon)
                    .stream()
                    .map(adv -> SearchDTO.builder()
                            .id(adv.id())
                            .title(adv.title())
                            .lat(adv.advLat())
                            .lon(adv.advLon())
                            .type("ADVENTURE")
                            .build())
                    .toList();
            case "LIST" -> listRepository.findByTitleContainingIgnoreCase(term)
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
}
