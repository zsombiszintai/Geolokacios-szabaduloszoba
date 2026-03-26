package com.cityscape.geoszabaduloszobabackend.api;

import com.cityscape.geoszabaduloszobabackend.model.dto.AdventureProfileDTO;
import com.cityscape.geoszabaduloszobabackend.model.dto.NearbyAdventureDTO;
import com.cityscape.geoszabaduloszobabackend.service.AdventureService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/adventures")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:5173")
public class AdventureAPI {

    private final AdventureService adventureService;

    @GetMapping("/map")
    public List<NearbyAdventureDTO> getMapData(
            @RequestParam Double lat,
            @RequestParam Double lon) {
        return adventureService.searchAndMap(null, lat, lon);
    }

    @GetMapping("/search")
    public List<NearbyAdventureDTO> search(
            @RequestParam String query,
            @RequestParam Double lat,
            @RequestParam Double lon) {
        return adventureService.searchAndMap(query, lat, lon);
    }

    @GetMapping("/{id}")
    public AdventureProfileDTO getAdventureDetails(
            @PathVariable Long id,
            @RequestParam Double lat,
            @RequestParam Double lon) {

        return adventureService.getDetails(id, lat, lon);
    }
}
