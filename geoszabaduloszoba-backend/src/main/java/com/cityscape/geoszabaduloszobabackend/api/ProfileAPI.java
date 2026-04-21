package com.cityscape.geoszabaduloszobabackend.api;

import com.cityscape.geoszabaduloszobabackend.model.dto.AdventureListDTO;
import com.cityscape.geoszabaduloszobabackend.model.view.UserAdventureStatistics;
import com.cityscape.geoszabaduloszobabackend.service.ProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/profile")
@RequiredArgsConstructor
public class ProfileAPI {

    private final ProfileService profileService;

    @GetMapping("/me")
    public UserAdventureStatistics getMyProfile(@AuthenticationPrincipal Jwt jwt) {
        return profileService.getMyStats(jwt.getSubject());
    }

    @GetMapping("/user/{username}")
    public UserAdventureStatistics getUserProfile(@PathVariable String username) {
        return profileService.getUserStats(username);
    }

    @GetMapping("/list")
    public List<AdventureListDTO> getList(@RequestParam String type, @AuthenticationPrincipal Jwt jwt) {

        return profileService.getListByType(jwt.getSubject(), type);
    }
}
