package com.cityscape.geoszabaduloszobabackend.api;

import com.cityscape.geoszabaduloszobabackend.model.dto.AdventureCreateDTO;
import com.cityscape.geoszabaduloszobabackend.model.entity.UserEntity;
import com.cityscape.geoszabaduloszobabackend.service.AdventureService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/create-adventure")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:5173")
public class CreateAdventureAPI {

    private final AdventureService adventureService;

    @PostMapping
    public Long create(@RequestBody AdventureCreateDTO dto,
                                       @AuthenticationPrincipal Jwt jwt) {

        return adventureService.createAdventureWithStations(dto, jwt);
    }
}
