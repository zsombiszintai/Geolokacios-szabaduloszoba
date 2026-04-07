package com.cityscape.geoszabaduloszobabackend.api;

import com.cityscape.geoszabaduloszobabackend.model.dto.CompletedAdventureDTO;
import com.cityscape.geoszabaduloszobabackend.service.CompletedAdventureService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/completed-adventures")
@RequiredArgsConstructor
public class CompletedAdventureAPI {

    private final CompletedAdventureService completedAdventureService;

    @GetMapping
    public List<CompletedAdventureDTO> getMyFinishedAdventures(Principal principal) {
        return completedAdventureService.getUserCompletedAdventures(principal.getName());
    }
}