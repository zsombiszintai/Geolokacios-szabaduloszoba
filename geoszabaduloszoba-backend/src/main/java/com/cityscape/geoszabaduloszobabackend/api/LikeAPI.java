package com.cityscape.geoszabaduloszobabackend.api;

import com.cityscape.geoszabaduloszobabackend.model.dto.LikedItemDTO;
import com.cityscape.geoszabaduloszobabackend.model.enums.LikeType;
import com.cityscape.geoszabaduloszobabackend.service.LikeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/likes")
@RequiredArgsConstructor
public class LikeAPI {
    private final LikeService likeService;

    @GetMapping("/mine/{type}")
    public List<LikedItemDTO> getMyLikes(@PathVariable LikeType type) {
        return likeService.getMyLikes(type);
    }

    @GetMapping("/{type}/{likedId}")
    public boolean isLiked(@PathVariable LikeType type, @PathVariable Long likedId) {
        return likeService.isLiked(type, likedId);
    }

    @PutMapping("/{type}/{likedId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void like(@PathVariable LikeType type, @PathVariable Long likedId) {
        likeService.like(type, likedId);
    }

    @DeleteMapping("/{type}/{likedId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unlike(@PathVariable LikeType type, @PathVariable Long likedId) {
        likeService.unlike(type, likedId);
    }
}
