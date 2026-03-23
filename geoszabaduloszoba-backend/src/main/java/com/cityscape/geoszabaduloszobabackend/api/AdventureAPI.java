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
    public ResponseEntity<List<NearbyAdventureDTO>> getMapData(
            @RequestParam Double lat,
            @RequestParam Double lon) {
        return ResponseEntity.ok(adventureService.searchAndMap(null, lat, lon));
    }

    @GetMapping("/search")
    public ResponseEntity<List<NearbyAdventureDTO>> search(
            @RequestParam String query,
            @RequestParam Double lat,
            @RequestParam Double lon) {
        return ResponseEntity.ok(adventureService.searchAndMap(query, lat, lon));
    }

    @GetMapping("/{id}")
    public ResponseEntity<AdventureProfileDTO> getAdventureDetails(
            @PathVariable Long id,
            @RequestParam Double lat,
            @RequestParam Double lon) {

        AdventureProfileDTO details = adventureService.getDetails(id, lat, lon);
        return ResponseEntity.ok(details);
    }
}
