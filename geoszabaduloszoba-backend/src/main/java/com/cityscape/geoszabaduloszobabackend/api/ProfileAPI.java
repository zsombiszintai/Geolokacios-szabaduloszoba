package com.cityscape.geoszabaduloszobabackend.api;

import com.cityscape.geoszabaduloszobabackend.model.view.UserAdventureStatistics;
import com.cityscape.geoszabaduloszobabackend.service.ProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
}
