package com.cityscape.geoszabaduloszobabackend.api;

import com.cityscape.geoszabaduloszobabackend.model.dto.AbandonedAdventureDTO;
import com.cityscape.geoszabaduloszobabackend.model.dto.AdventureProfileDTO;
import com.cityscape.geoszabaduloszobabackend.model.dto.NearbyAdventureDTO;
import com.cityscape.geoszabaduloszobabackend.service.AdventureService;
import com.cityscape.geoszabaduloszobabackend.model.dto.PageDTO;
import com.cityscape.geoszabaduloszobabackend.api.mapper.ApiMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/adventures")
@RequiredArgsConstructor
public class AdventureAPI {

    private final ApiMapper apiMapper;

    private final AdventureService adventureService;

    @GetMapping("/map")
    public List<NearbyAdventureDTO> getMapData(
            @RequestParam Double lat,
            @RequestParam Double lon) {
        return adventureService.searchNearby(null, lat, lon).stream().map(apiMapper::toDto).toList();
    }

    @GetMapping("/search")
    public List<NearbyAdventureDTO> search(
            @RequestParam String query,
            @RequestParam Double lat,
            @RequestParam Double lon) {
        return adventureService.searchNearby(query, lat, lon).stream().map(apiMapper::toDto).toList();
    }

    @GetMapping("/{id}")
    public AdventureProfileDTO getAdventureDetails(
            @PathVariable("id") Long id,
            @RequestParam(value = "lat", required = false) Double lat,
            @RequestParam(value = "lon", required = false) Double lon) {

        return apiMapper.toDto(adventureService.getDetails(id, lat, lon));
    }

    @GetMapping("/abandoned-all")
    public List<AbandonedAdventureDTO> getAllAbandoned() {
        return adventureService.getAllAbandonedByUser().stream().map(apiMapper::toDto).toList();
    }

    @GetMapping("/search/page")
    public PageDTO<NearbyAdventureDTO> searchPage(
            @RequestParam String query,
            @RequestParam Double lat,
            @RequestParam Double lon,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return PageDTO.from(adventureService.searchNearbyPage(query, lat, lon, page, size).map(apiMapper::toDto));
    }
}
