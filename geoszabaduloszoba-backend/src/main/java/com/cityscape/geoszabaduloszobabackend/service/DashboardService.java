package com.cityscape.geoszabaduloszobabackend.service;

import com.cityscape.geoszabaduloszobabackend.model.domain.NearbyAdventureResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final AdventureService adventureService;

    public List<NearbyAdventureResult> getNearbyTop3(Double lat, Double lon) {
        List<NearbyAdventureResult> allNearby = adventureService.searchNearby(null, lat, lon);

        return allNearby.stream()
                .sorted(Comparator.comparingInt(NearbyAdventureResult::distanceInMeters))
                .limit(3)
                .toList();
    }
}