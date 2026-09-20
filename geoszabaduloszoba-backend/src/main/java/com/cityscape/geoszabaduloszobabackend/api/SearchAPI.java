package com.cityscape.geoszabaduloszobabackend.api;

import com.cityscape.geoszabaduloszobabackend.model.dto.NearbyAdventureDTO;
import com.cityscape.geoszabaduloszobabackend.model.dto.SearchDTO;
import com.cityscape.geoszabaduloszobabackend.service.SearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/search")
@RequiredArgsConstructor
public class SearchAPI {

    private final SearchService searchService;

    @GetMapping
    public List<SearchDTO> search(
            @RequestParam("q") String q,
            @RequestParam("type") String type,
            @RequestParam(value = "lat", required = false) Double lat,
            @RequestParam(value = "lon", required = false) Double lon) {
        return searchService.searchEverything(q, type, lat, lon);
    }
}
