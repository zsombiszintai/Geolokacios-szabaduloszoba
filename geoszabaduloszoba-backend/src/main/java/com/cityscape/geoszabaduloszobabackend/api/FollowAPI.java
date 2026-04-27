package com.cityscape.geoszabaduloszobabackend.api;

import com.cityscape.geoszabaduloszobabackend.service.FollowService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/follows")
@RequiredArgsConstructor
public class FollowAPI {

    private final FollowService followService;

    @PostMapping("/{followedId}")
    public void follow(@AuthenticationPrincipal Jwt jwt, @PathVariable Long followedId) {
        followService.followUser(jwt.getSubject(), followedId);
    }

    @DeleteMapping("/{followedId}")
    public void unfollow(@AuthenticationPrincipal Jwt jwt, @PathVariable Long followedId) {
        followService.unfollowUser(jwt.getSubject(), followedId);
    }

    @GetMapping("/is-following/{followedId}")
    public boolean isFollowing(@AuthenticationPrincipal Jwt jwt, @PathVariable Long followedId) {
        return followService.isFollowing(jwt.getSubject(), followedId);
    }
}
