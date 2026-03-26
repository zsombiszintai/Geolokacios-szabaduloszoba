package com.cityscape.geoszabaduloszobabackend.api;

import com.cityscape.geoszabaduloszobabackend.model.dto.NearbyAdventureDTO;
import com.cityscape.geoszabaduloszobabackend.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:5173")
public class DashboardAPI {

    private final DashboardService dashboardService;

    @GetMapping
    public List<NearbyAdventureDTO> getNearby(
            @RequestParam Double lat,
            @RequestParam Double lon) {
        return dashboardService.getNearbyTop3(lat, lon);
    }
}