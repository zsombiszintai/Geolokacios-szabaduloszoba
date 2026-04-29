package com.cityscape.geoszabaduloszobabackend.api;

import com.cityscape.geoszabaduloszobabackend.model.dto.AbandonedAdventureDTO;
import com.cityscape.geoszabaduloszobabackend.model.dto.AdventureProfileDTO;
import com.cityscape.geoszabaduloszobabackend.model.dto.NearbyAdventureDTO;
import com.cityscape.geoszabaduloszobabackend.service.AdventureService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
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

    @GetMapping("/abandoned-all")
    public List<AbandonedAdventureDTO> getAllAbandoned(@AuthenticationPrincipal Jwt jwt) {
        return adventureService.getAllAbandonedByUser(jwt.getSubject());
    }

}
