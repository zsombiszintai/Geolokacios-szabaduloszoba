package com.cityscape.geoszabaduloszobabackend.service;

import com.cityscape.geoszabaduloszobabackend.mapper.AbstractMapper;
import com.cityscape.geoszabaduloszobabackend.model.dto.ReviewDTO;
import com.cityscape.geoszabaduloszobabackend.model.entity.AdventureEntity;
import com.cityscape.geoszabaduloszobabackend.model.entity.ReviewEntity;
import com.cityscape.geoszabaduloszobabackend.model.entity.UserEntity;
import com.cityscape.geoszabaduloszobabackend.repository.AdventureRepository;
import com.cityscape.geoszabaduloszobabackend.repository.ReviewRepository;
import com.cityscape.geoszabaduloszobabackend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final UserRepository userRepository;
    private final AdventureRepository adventureRepository;
    private final ReviewServiceMapper mapper;

    public void saveReview(String keycloakSub, ReviewDTO dto) {
        var user = userRepository.findByKeycloakSub(keycloakSub)
                .orElseThrow(() -> new RuntimeException("Felhasználó nem található"));

        var adventure = adventureRepository.findById(dto.adventureId())
                .orElseThrow(() -> new RuntimeException("Kaland nem található"));

        var review = mapper.buildEntity(dto, user, adventure);
        reviewRepository.save(review);
    }

    public List<ReviewDTO> getUserReviews(String keycloakSub) {
        var user = userRepository.findByKeycloakSub(keycloakSub)
                .orElseThrow(() -> new RuntimeException("Felhasználó nem található"));

        return reviewRepository.findByUserOrderByReviewedAtDesc(user).stream()
                .map(mapper::mapDTO)
                .toList();
    }

    @Mapper(config = AbstractMapper.class, imports = {LocalDate.class})
    public interface ReviewServiceMapper extends AbstractMapper<ReviewEntity, ReviewDTO> {

        @Override
        @Mapping(target = "adventureId", source = "adventure.id")
        @Mapping(target = "adventureTitle", source = "adventure.title")
        ReviewDTO mapDTO(ReviewEntity entity);

        @Mapping(target = "id", ignore = true)
        @Mapping(target = "user", source = "user")
        @Mapping(target = "adventure", source = "adventure")
        @Mapping(target = "rating", source = "dto.rating")
        @Mapping(target = "reviewText", source = "dto.reviewText")
        @Mapping(target = "reviewedAt", expression = "java(LocalDate.now())")
        ReviewEntity buildEntity(ReviewDTO dto, UserEntity user, AdventureEntity adventure);
    }
}