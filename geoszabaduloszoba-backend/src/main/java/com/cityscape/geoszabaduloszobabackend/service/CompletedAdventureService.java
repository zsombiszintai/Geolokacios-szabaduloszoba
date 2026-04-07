package com.cityscape.geoszabaduloszobabackend.service;

import com.cityscape.geoszabaduloszobabackend.mapper.AbstractMapper;
import com.cityscape.geoszabaduloszobabackend.model.dto.CompletedAdventureDTO;
import com.cityscape.geoszabaduloszobabackend.model.entity.CompletedAdventureEntity;
import com.cityscape.geoszabaduloszobabackend.model.entity.ReviewEntity;
import com.cityscape.geoszabaduloszobabackend.model.entity.UserEntity;
import com.cityscape.geoszabaduloszobabackend.repository.CompletedAdventureRepository;
import com.cityscape.geoszabaduloszobabackend.repository.ReviewRepository;
import com.cityscape.geoszabaduloszobabackend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class CompletedAdventureService {

    private final CompletedAdventureRepository completedRepository;
    private final ReviewRepository reviewRepository;
    private final UserRepository userRepository;
    private final CompletedMapper mapper;

    public List<CompletedAdventureDTO> getUserCompletedAdventures(String keycloakSub) {
        UserEntity user = userRepository.findByKeycloakSub(keycloakSub)
                .orElseThrow(() -> new RuntimeException("Felhasználó nem található"));

        var completedList = completedRepository.findCompletedAdventureEntitiesByUserId(user.getId());

        var allReviews = reviewRepository.findByUserOrderByReviewedAtDesc(user);

        return completedList.stream()
                .map(completed -> {
                    var review = allReviews.stream()
                            .filter(r -> r.getAdventure().getId().equals(completed.getAdventure().getId()))
                            .findFirst()
                            .orElse(null);

                    return mapper.toCompletedDTO(completed, review);
                })
                .toList();
    }

    @Mapper(config = AbstractMapper.class)
    public interface CompletedMapper {
        @Mapping(target = "id", source = "completed.id")
        @Mapping(target = "adventureId", source = "completed.adventure.id")
        @Mapping(target = "adventureTitle", source = "completed.adventure.title")
        @Mapping(target = "completedAt", source = "completed.completedAt")
        @Mapping(target = "distanceTravelled", source = "completed.distanceTravelled")
        @Mapping(target = "durationSec", source = "completed.durationSec")
        @Mapping(target = "rating", source = "review.rating")
        CompletedAdventureDTO toCompletedDTO(CompletedAdventureEntity completed, ReviewEntity review);
    }
}