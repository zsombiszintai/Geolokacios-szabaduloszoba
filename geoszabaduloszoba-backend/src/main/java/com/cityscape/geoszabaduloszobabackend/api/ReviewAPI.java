package com.cityscape.geoszabaduloszobabackend.api;

import com.cityscape.geoszabaduloszobabackend.model.dto.ReviewDTO;
import com.cityscape.geoszabaduloszobabackend.service.ReviewService;
import com.cityscape.geoszabaduloszobabackend.api.mapper.ApiMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
public class ReviewAPI {

    private final ApiMapper apiMapper;

    private final ReviewService reviewService;

    @PostMapping
    public void postReview(@RequestBody ReviewDTO dto) {
        reviewService.saveReview(dto.adventureId(), apiMapper.toReviewEntity(dto));
    }

    @GetMapping("/my")
    public List<ReviewDTO> getMyReviews() {
        return reviewService.getUserReviews().stream().map(apiMapper::toDto).toList();
    }
}