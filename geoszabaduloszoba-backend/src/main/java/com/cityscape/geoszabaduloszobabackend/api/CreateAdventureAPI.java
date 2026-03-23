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
    public ResponseEntity<Long> create(@RequestBody AdventureCreateDTO dto,
                                       @AuthenticationPrincipal Jwt jwt) {

        String userSub = jwt.getSubject();
        String username = jwt.getClaimAsString("preferred_username");

        Long adventureId = adventureService.createAdventureWithStations(dto, jwt);

        return ResponseEntity.ok(adventureId);
    }
}
