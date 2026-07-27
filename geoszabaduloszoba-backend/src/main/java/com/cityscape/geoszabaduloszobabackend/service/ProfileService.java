package com.cityscape.geoszabaduloszobabackend.service;

import com.cityscape.geoszabaduloszobabackend.model.dto.AdventureListDTO;
import com.cityscape.geoszabaduloszobabackend.model.dto.ReviewDTO;
import com.cityscape.geoszabaduloszobabackend.model.dto.UserListDTO;
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
    private final FollowRepository followRepository;
    private final AvatarStorageService avatarStorageService;
    private final UserService userService;
    private final UserRepository userRepository;

    @Transactional
    public UserAdventureStatistics getMyStats(String keycloakSub) {

        UserEntity currentUser = userService.getOrCreateCurrentUser();

        UserAdventureStatistics stats = statsRepository.findByKeycloakSub(currentUser.getKeycloakSub())
                .orElseThrow(() -> new RuntimeException("User not found: " + currentUser.getKeycloakSub()));

        stats.setProfilePictureUrl(formatAvatarUrl(stats.getProfilePictureUrl()));
        return stats;
    }

    @Transactional
    public UserAdventureStatistics getUserStats(String username) {

        userService.getOrCreateCurrentUser();

        UserAdventureStatistics stats = statsRepository.findByUsername(username)
                .orElseGet(() -> {
                    UserEntity targetUser = userRepository.findByUsername(username)
                            .orElseThrow(() -> new RuntimeException("User not found: " + username));
                    return createEmptyStats(targetUser);
                });

        stats.setProfilePictureUrl(formatAvatarUrl(stats.getProfilePictureUrl()));
        return stats;
    }

    public List<?> getListByType(String sub, String type) {
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
                    .map(this::mapToRatedDTO)
                    .toList();

            case "reviewed" -> reviewRepository.findAllByUserKeycloakSub(sub).stream()
                    .map(this::mapReviewToDTO).toList();

            case "followers" -> followRepository.findAllByFollowedKeycloakSub(sub).stream()
                    .map(follow -> mapUserToDTO(follow.getFollower()))
                    .toList();

            case "following" -> followRepository.findAllByFollowerKeycloakSub(sub).stream()
                    .map(follow -> mapUserToDTO(follow.getFollowed()))
                    .toList();
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

    private ReviewDTO mapToRatedDTO(ReviewEntity review) {
        return new ReviewDTO(
                review.getId(),
                review.getAdventure().getId(),
                review.getAdventure().getTitle(),
                review.getRating(),
                review.getReviewText(),
                review.getReviewedAt()
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

    private UserListDTO mapUserToDTO(UserEntity user) {
        String finalUrl = user.getProfilePictureUrl();

        if (finalUrl != null && !finalUrl.startsWith("http")) {
            finalUrl = avatarStorageService.publicUrl(finalUrl);
        }

        return new UserListDTO(
                user.getId(),
                user.getUsername(),
                user.getProfileDescription(),
                finalUrl
        );
    }

    private String formatAvatarUrl(String urlOrKey) {
        if (urlOrKey == null || urlOrKey.isBlank()) {
            return null;
        }
        if (urlOrKey.startsWith("http")) {
            return urlOrKey;
        }
        return avatarStorageService.publicUrl(urlOrKey);
    }

    private UserAdventureStatistics createEmptyStats(UserEntity user) {
        UserAdventureStatistics emptyStats = new UserAdventureStatistics();
        emptyStats.setKeycloakSub(user.getKeycloakSub());
        emptyStats.setUsername(user.getUsername());
        emptyStats.setProfilePictureUrl(user.getProfilePictureUrl());
        emptyStats.setProfileDescription(user.getProfileDescription());

        return emptyStats;
    }

}
