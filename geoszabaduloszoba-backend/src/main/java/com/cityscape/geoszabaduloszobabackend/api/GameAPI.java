package com.cityscape.geoszabaduloszobabackend.api;

import com.cityscape.geoszabaduloszobabackend.model.dto.ActiveGameDTO;
import com.cityscape.geoszabaduloszobabackend.model.dto.GameSessionDTO;
import com.cityscape.geoszabaduloszobabackend.service.GameService;
import com.cityscape.geoszabaduloszobabackend.api.mapper.ApiMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/game")
@RequiredArgsConstructor
public class GameAPI {
    private final GameService gameService;
    private final ApiMapper apiMapper;

    @PostMapping("/update")
    public void updateStatus(
            @RequestBody ActiveGameDTO dto) {
        gameService.updateActiveGame(apiMapper.toGameUpdate(dto));
    }

    @PostMapping("/finish")
    public void finish(
            @RequestBody ActiveGameDTO dto) {
        gameService.finishGame(apiMapper.toGameUpdate(dto));
    }

    @PostMapping("/start/{adventureId}")
    public Long start(@PathVariable Long adventureId) {
        return gameService.startGame(adventureId);
    }

    @GetMapping("/session/{sessionId}")
    public GameSessionDTO getSession(
            @PathVariable("sessionId") Long sessionId) {
        return apiMapper.toDto(gameService.getSession(sessionId));
    }
}
