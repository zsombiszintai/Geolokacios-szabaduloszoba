package com.cityscape.geoszabaduloszobabackend.api;

import com.cityscape.geoszabaduloszobabackend.model.dto.NearbyAdventureDTO;
import com.cityscape.geoszabaduloszobabackend.service.DashboardService;
import com.cityscape.geoszabaduloszobabackend.api.mapper.ApiMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardAPI {

    private final ApiMapper apiMapper;

    private final DashboardService dashboardService;

    @GetMapping
    public List<NearbyAdventureDTO> getNearby(
            @RequestParam Double lat,
            @RequestParam Double lon) {
        return dashboardService.getNearbyTop3(lat, lon).stream().map(apiMapper::toDto).toList();
    }
}