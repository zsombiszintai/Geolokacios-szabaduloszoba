package com.cityscape.geoszabaduloszobabackend.service;

import com.cityscape.geoszabaduloszobabackend.model.dto.AdventureListDTO;
import com.cityscape.geoszabaduloszobabackend.model.entity.AdventureEntity;
import com.cityscape.geoszabaduloszobabackend.model.entity.ReviewEntity;
import com.cityscape.geoszabaduloszobabackend.model.entity.UserEntity;
import com.cityscape.geoszabaduloszobabackend.model.view.UserAdventureStatistics;
import com.cityscape.geoszabaduloszobabackend.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProfileService {

    private final UserAdventureStatisticsRepository statsRepository;
    private final AdventureRepository adventureRepository;
    private final CompletedAdventureRepository completedRepository;
    private final AbandonedAdventureRepository abandonedRepository;
    private final ReviewRepository reviewRepository;

    @Transactional(readOnly = true)
    public UserAdventureStatistics getMyStats(String keycloakSub) {
        return statsRepository.findByKeycloakSub(keycloakSub)
                .orElseThrow(() -> new RuntimeException("User not found:" + keycloakSub));
    }

    @Transactional(readOnly = true)
    public UserAdventureStatistics getUserStats(String username) {
        return statsRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found:" + username));
    }

    public List<AdventureListDTO> getListByType(String sub, String type) {
        return switch (type) {
            case "completed-adventure" -> completedRepository.findAllByUserKeycloakSub(sub).stream()
                    .map(entity -> mapToDTO(entity.getAdventure()))
                    .toList();

            case "abandoned-adventure" -> abandonedRepository.findAllByUserKeycloakSub(sub).stream()
                    .map(entity -> mapToDTO(entity.getAdventure()))
                    .toList();
            case "created" -> adventureRepository.findAllByCreatorKeycloakSub(sub).stream()
                    .map(this::mapToDTO)
                    .toList();
            case "rated" -> reviewRepository.findAllByUserKeycloakSubAndRatingIsNotNull(sub).stream()
                    .map(review -> mapToDTO(review.getAdventure()))
                    .toList();
            case "reviewed" -> reviewRepository.findAllByUserKeycloakSub(sub).stream()
                    .map(this::mapReviewToDTO).toList();
            default -> Collections.emptyList();
        };
    }


    private AdventureListDTO mapToDTO(AdventureEntity adventure) {
        if (adventure == null) return null;

        return new AdventureListDTO(
                adventure.getId(),
                adventure.getTitle(),
                adventure.getDescription(),
                adventure.getDifficulty()
        );
    }

    private AdventureListDTO mapReviewToDTO(ReviewEntity review) {
        AdventureEntity adv = review.getAdventure();
        return new AdventureListDTO(
                adv.getId(),
                adv.getTitle(),
                review.getReviewText(),
                adv.getDifficulty()
        );
    }

}
