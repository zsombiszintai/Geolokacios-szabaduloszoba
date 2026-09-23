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
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.BeanUtils;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Collections;
import java.util.List;

@Slf4j
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
    private final KeycloakAdminService keycloakAdminService;

    @Transactional
    public UserAdventureStatistics getMyStats(String keycloakSub) {
        UserEntity currentUser = userService.getOrCreateCurrentUser();

        return buildProfile(
                currentUser.getKeycloakSub(),
                userService.getCurrentUsername()
        );
    }

    @Transactional(readOnly = true)
    public UserAdventureStatistics getUserStats(String username) {
        var keycloakUser = keycloakAdminService.getByUsername(username);

        return buildProfile(
                keycloakUser.getId(),
                keycloakUser.getUsername()
        );
    }

    private UserAdventureStatistics buildProfile(
            String keycloakSub,
            String username
    ) {
        UserAdventureStatistics storedStats =
                statsRepository.findByKeycloakSub(keycloakSub)
                        .orElseThrow(() -> new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Ehhez a felhasználóhoz nincs helyi profilstatisztika."
                        ));

        UserAdventureStatistics response = new UserAdventureStatistics();
        BeanUtils.copyProperties(storedStats, response);

        response.setUsername(username);
        response.setProfilePictureUrl(
                formatAvatarUrl(storedStats.getProfilePictureUrl())
        );

        return response;
    }

    @Transactional(readOnly = true)
    public List<?> getListByType(
            String currentSub,
            String username,
            String type
    ) {
        String targetSub = currentSub;

        if (username != null && !username.isBlank()) {
            targetSub = keycloakAdminService
                    .getByUsername(username)
                    .getId();
        }

        return switch (type) {

            case "completed-adventure" -> completedRepository.findAllByUserKeycloakSub(targetSub).stream()
                    .map(entity -> mapToDTO(entity.getAdventure()))
                    .toList();

            case "abandoned-adventure" -> abandonedRepository.findAllByUserKeycloakSub(targetSub).stream()
                    .map(entity -> mapToDTO(entity.getAdventure()))
                    .toList();

            case "created" -> adventureRepository.findAllByCreatorKeycloakSub(targetSub).stream()
                    .map(this::mapToDTO)
                    .toList();

            case "rated" -> reviewRepository.findAllByUserKeycloakSubAndRatingIsNotNull(targetSub).stream()
                    .map(this::mapToRatedDTO)
                    .toList();

            case "reviewed" -> reviewRepository.findAllByUserKeycloakSub(targetSub).stream()
                    .map(this::mapReviewToDTO).toList();

            case "followers" -> followRepository.findAllByFollowedKeycloakSub(targetSub).stream()
                    .map(follow -> mapUserToDTO(follow.getFollower()))
                    .toList();

            case "following" -> followRepository.findAllByFollowerKeycloakSub(targetSub).stream()
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
        return new UserListDTO(
                user.getId(),
                resolveUsername(user),
                user.getProfileDescription(),
                formatAvatarUrl(user.getProfilePictureUrl())
        );
    }

    private String formatAvatarUrl(String urlOrKey) {
        if (urlOrKey == null
                || urlOrKey.isBlank()
                || "/images/default-avatar.png".equals(urlOrKey)
                || "images/default-avatar.png".equals(urlOrKey)) {
            return null;
        }
        if (urlOrKey.startsWith("http")) {
            return urlOrKey;
        }
        return avatarStorageService.publicUrl(urlOrKey);
    }

    private String resolveUsername(UserEntity user) {
        String sub = user.getKeycloakSub();

        if (sub == null || sub.isBlank()) {
            log.warn(
                    "Hiányzó Keycloak-azonosító a követési listában. userId={}",
                    user.getId()
            );
            return "Nem elérhető felhasználó";
        }

        try {
            return keycloakAdminService.getUsername(sub);
        } catch (ResponseStatusException exception) {
            if (exception.getStatusCode().value() != 404) {
                throw exception;
            }

            log.warn(
                    "Keycloak-felhasználó lekérdezése 404-et adott. userId={}, sub={}",
                    user.getId(),
                    sub
            );

            return "Nem elérhető felhasználó";
        }
    }
}
