package com.cityscape.geoszabaduloszobabackend.service;

import com.cityscape.geoszabaduloszobabackend.model.dto.NearbyAdventureDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final AdventureService adventureService;

    public List<NearbyAdventureDTO> getNearbyTop3(Double lat, Double lon) {
        List<NearbyAdventureDTO> allNearby = adventureService.searchAndMap(null, lat, lon);

        return allNearby.stream()
                .sorted(Comparator.comparingInt(NearbyAdventureDTO::distanceInMeters))
                .limit(3)
                .toList();
    }
}