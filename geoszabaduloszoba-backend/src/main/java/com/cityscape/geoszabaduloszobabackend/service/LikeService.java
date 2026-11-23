package com.cityscape.geoszabaduloszobabackend.service;

import com.cityscape.geoszabaduloszobabackend.model.entity.LikeEntity;
import com.cityscape.geoszabaduloszobabackend.model.entity.UserEntity;
import com.cityscape.geoszabaduloszobabackend.model.enums.LikeType;
import com.cityscape.geoszabaduloszobabackend.repository.AdventureRepository;
import com.cityscape.geoszabaduloszobabackend.repository.LikeRepository;
import com.cityscape.geoszabaduloszobabackend.repository.ListRepository;
import com.cityscape.geoszabaduloszobabackend.repository.UserRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class LikeService {
    private final LikeRepository likeRepository;
    private final AdventureRepository adventureRepository;
    private final ListRepository listRepository;
    private final UserRepository userRepository;
    private final UserService userService;
    private final EntityManager entityManager;

    @Transactional(readOnly = true)
    public boolean isLiked(LikeType type, Long likedId) {
        requireTarget(type, likedId);
        return userRepository.findByKeycloakSub(userService.getCurrentUserId())
                .map(user -> likeRepository.existsByLiker_IdAndLikedIdAndType(user.getId(), likedId, type))
                .orElse(false);
    }

    @Transactional
    public void like(LikeType type, Long likedId) {
        requireTarget(type, likedId);
        UserEntity liker = userService.getOrCreateCurrentUser();
        entityManager.lock(liker, LockModeType.PESSIMISTIC_WRITE);
        if (!likeRepository.existsByLiker_IdAndLikedIdAndType(liker.getId(), likedId, type)) {
            likeRepository.save(new LikeEntity(liker, likedId, type));
        }
    }

    @Transactional
    public void unlike(LikeType type, Long likedId) {
        userRepository.findByKeycloakSub(userService.getCurrentUserId())
                .ifPresent(liker -> {
                    entityManager.lock(liker, LockModeType.PESSIMISTIC_WRITE);
                    likeRepository.deleteByLiker_IdAndLikedIdAndType(liker.getId(), likedId, type);
                });
    }

    private void requireTarget(LikeType type, Long likedId) {
        boolean exists = switch (type) {
            case ADVENTURE -> adventureRepository.existsById(likedId);
            case LIST -> listRepository.existsById(likedId);
        };
        if (!exists) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "A kedvelés célpontja nem található.");
        }
    }
}
