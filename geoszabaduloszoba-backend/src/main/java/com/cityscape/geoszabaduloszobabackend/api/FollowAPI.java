package com.cityscape.geoszabaduloszobabackend.api;

import com.cityscape.geoszabaduloszobabackend.service.FollowService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/follows")
@RequiredArgsConstructor
public class FollowAPI {

    private final FollowService followService;

    @PostMapping("/{followedId}")
    public void follow(@PathVariable Long followedId) {
        followService.followUser(followedId);
    }

    @DeleteMapping("/{followedId}")
    public void unfollow(@PathVariable Long followedId) {
        followService.unfollowUser(followedId);
    }

    @GetMapping("/is-following/{followedId}")
    public boolean isFollowing(@PathVariable Long followedId) {
        return followService.isFollowing(followedId);
    }
}
