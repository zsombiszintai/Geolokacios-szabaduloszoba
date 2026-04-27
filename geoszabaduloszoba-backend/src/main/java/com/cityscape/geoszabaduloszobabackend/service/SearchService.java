package com.cityscape.geoszabaduloszobabackend.service;

import com.cityscape.geoszabaduloszobabackend.model.dto.NearbyAdventureDTO;
import com.cityscape.geoszabaduloszobabackend.model.entity.AdventureEntity;
import com.cityscape.geoszabaduloszobabackend.repository.AdventureRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SearchService {

    private final AdventureService adventureService;

    public List<NearbyAdventureDTO> searchNearbyAdventures(String query, Double lat, Double lon) {
        List<NearbyAdventureDTO> foundAdventures = adventureService.searchAndMap(query, lat, lon);

        return foundAdventures.stream()
                .sorted(Comparator.comparingInt(NearbyAdventureDTO::distanceInMeters))
                .toList();
    }
}
