package com.cityscape.geoszabaduloszobabackend.api;

import com.cityscape.geoszabaduloszobabackend.model.dto.CompletedAdventureDTO;
import com.cityscape.geoszabaduloszobabackend.service.CompletedAdventureService;
import com.cityscape.geoszabaduloszobabackend.api.mapper.ApiMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/completed-adventures")
@RequiredArgsConstructor
public class CompletedAdventureAPI {

    private final ApiMapper apiMapper;

    private final CompletedAdventureService completedAdventureService;

    @GetMapping
    public List<CompletedAdventureDTO> getMyFinishedAdventures() {
        return completedAdventureService.getUserCompletedAdventures().stream().map(apiMapper::toDto).toList();
    }
}