package com.cityscape.geoszabaduloszobabackend.service;

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

import com.cityscape.geoszabaduloszobabackend.model.domain.UserSummary;
import com.cityscape.geoszabaduloszobabackend.model.domain.ReviewedAdventure;
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
    private final KeycloakAdminService keycloakAdminService;

    @Transactional
    public UserAdventureStatistics getMyStats() {
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
            String username,
            String type
    ) {
        String targetSub = userService.getCurrentUserId();

        if (username != null && !username.isBlank()) {
            targetSub = keycloakAdminService
                    .getByUsername(username)
                    .getId();
        }

        return switch (type) {

            case "completed-adventure" -> completedRepository.findAllByUserKeycloakSub(targetSub).stream()
                    .map(entity -> entity.getAdventure())
                    .toList();

            case "abandoned-adventure" -> abandonedRepository.findAllByUserKeycloakSub(targetSub).stream()
                    .map(entity -> entity.getAdventure())
                    .toList();

            case "created" -> adventureRepository.findAllByCreatorKeycloakSub(targetSub).stream()
                    .toList();

            case "rated" -> reviewRepository.findAllByUserKeycloakSubAndRatingIsNotNull(targetSub).stream()
                    .toList();

            case "reviewed" -> reviewRepository.findAllByUserKeycloakSub(targetSub).stream()
                    .map(review -> new ReviewedAdventure(review.getAdventure(), review.getReviewText())).toList();

            case "followers" -> followRepository.findAllByFollowedKeycloakSub(targetSub).stream()
                    .map(follow -> toUserResult(follow.getFollower()))
                    .toList();

            case "following" -> followRepository.findAllByFollowerKeycloakSub(targetSub).stream()
                    .map(follow -> toUserResult(follow.getFollowed()))
                    .toList();
            default -> Collections.emptyList();
        };
    }


    private UserSummary toUserResult(UserEntity user) {
        return new UserSummary(
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
