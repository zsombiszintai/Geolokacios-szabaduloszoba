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
@CrossOrigin(origins = "http://localhost:5173")
public class SearchAPI {

    private final SearchService searchService;

    @GetMapping
    public List<SearchDTO> search(
            @RequestParam String q,
            @RequestParam String type,
            @RequestParam Double lat,
            @RequestParam Double lon) {
        return searchService.searchEverything(q, type, lat, lon);
    }
}
