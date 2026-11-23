package com.cityscape.geoszabaduloszobabackend.api;

import com.cityscape.geoszabaduloszobabackend.model.enums.LikeType;
import com.cityscape.geoszabaduloszobabackend.service.LikeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/likes/{type}/{likedId}")
@RequiredArgsConstructor
public class LikeAPI {
    private final LikeService likeService;

    @GetMapping
    public boolean isLiked(@PathVariable LikeType type, @PathVariable Long likedId) {
        return likeService.isLiked(type, likedId);
    }

    @PutMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void like(@PathVariable LikeType type, @PathVariable Long likedId) {
        likeService.like(type, likedId);
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unlike(@PathVariable LikeType type, @PathVariable Long likedId) {
        likeService.unlike(type, likedId);
    }
}
