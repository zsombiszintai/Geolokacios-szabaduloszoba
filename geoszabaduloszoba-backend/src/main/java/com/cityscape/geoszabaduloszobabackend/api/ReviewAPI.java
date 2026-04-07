package com.cityscape.geoszabaduloszobabackend.api;

import com.cityscape.geoszabaduloszobabackend.model.dto.ReviewDTO;
import com.cityscape.geoszabaduloszobabackend.service.ReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
public class ReviewAPI {

    private final ReviewService reviewService;

    @PostMapping
    public void postReview(Principal principal, @RequestBody ReviewDTO dto) {
        reviewService.saveReview(principal.getName(), dto);
    }

    @GetMapping("/my")
    public List<ReviewDTO> getMyReviews(Principal principal) {
        return reviewService.getUserReviews(principal.getName());
    }
}