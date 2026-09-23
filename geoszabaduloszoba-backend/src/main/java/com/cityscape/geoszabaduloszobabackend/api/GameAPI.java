package com.cityscape.geoszabaduloszobabackend.api;

import com.cityscape.geoszabaduloszobabackend.model.dto.ActiveGameDTO;
import com.cityscape.geoszabaduloszobabackend.model.dto.GameSessionDTO;
import com.cityscape.geoszabaduloszobabackend.service.GameService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/game")
@RequiredArgsConstructor
public class GameAPI {
    private final GameService gameService;

    @PostMapping("/update")
    public void updateStatus(
            @RequestBody ActiveGameDTO dto,
            JwtAuthenticationToken token
    ) {
        gameService.updateActiveGame(dto, token.getName());
    }

    @PostMapping("/finish")
    public void finish(
            @RequestBody ActiveGameDTO dto,
            JwtAuthenticationToken token
    ) {
        gameService.finishGame(dto, token.getName());
    }

    @PostMapping("/start/{adventureId}")
    public Long start(@PathVariable Long adventureId, JwtAuthenticationToken token) {
        String sub = token.getName();
        return gameService.startGame(adventureId, sub);
    }

    @GetMapping("/session/{sessionId}")
    public GameSessionDTO getSession(
            @PathVariable("sessionId") Long sessionId,
            JwtAuthenticationToken token
    ) {
        return gameService.getSession(sessionId, token.getName());
    }
}
