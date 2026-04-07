package com.cityscape.geoszabaduloszobabackend.api;

import com.cityscape.geoszabaduloszobabackend.model.dto.ActiveGameDTO;
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
    public void updateStatus(@RequestBody ActiveGameDTO dto) {

        gameService.updateActiveGame(dto);
    }

    @PostMapping("/start/{adventureId}")
    public Long start(@PathVariable Long adventureId, JwtAuthenticationToken token) {
        String sub = token.getName();
        return gameService.startGame(adventureId, sub);
    }
}
