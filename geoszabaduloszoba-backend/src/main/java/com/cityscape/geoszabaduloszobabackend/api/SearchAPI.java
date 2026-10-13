package com.cityscape.geoszabaduloszobabackend.api;

import com.cityscape.geoszabaduloszobabackend.model.dto.PageDTO;
import com.cityscape.geoszabaduloszobabackend.model.dto.SearchDTO;
import com.cityscape.geoszabaduloszobabackend.service.SearchService;
import com.cityscape.geoszabaduloszobabackend.api.mapper.ApiMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/search")
@RequiredArgsConstructor
public class SearchAPI {

    private final ApiMapper apiMapper;

    private final SearchService searchService;

    @GetMapping
    public List<SearchDTO> search(
            @RequestParam("q") String q,
            @RequestParam("type") String type,
            @RequestParam(value = "lat", required = false) Double lat,
            @RequestParam(value = "lon", required = false) Double lon) {
        return searchService.searchEverything(q, type, lat, lon).stream().map(apiMapper::toDto).toList();
    }
    @GetMapping("/page")
    public PageDTO<SearchDTO> searchPage(
            @RequestParam("q") String query,
            @RequestParam("type") String type,
            @RequestParam(value = "lat", required = false) Double lat,
            @RequestParam(value = "lon", required = false) Double lon,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return PageDTO.from(searchService.searchPage(query, type, lat, lon, page, size).map(apiMapper::toDto));
    }
}
