package com.cityscape.geoszabaduloszobabackend.service;

import com.cityscape.geoszabaduloszobabackend.model.dto.LikedItemDTO;
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

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

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
    public List<LikedItemDTO> getMyLikes(LikeType type) {
        List<LikeEntity> likes = userRepository.findByKeycloakSub(userService.getCurrentUserId())
                .map(user -> likeRepository.findAllByLiker_IdAndTypeOrderByLikeDateDescIdDesc(user.getId(), type))
                .orElseGet(List::of);
        if (likes.isEmpty()) {
            return List.of();
        }

        List<Long> ids = likes.stream().map(LikeEntity::getLikedId).toList();
        List<LikedItemDTO> items = switch (type) {
            case ADVENTURE -> adventureRepository.findAllById(ids).stream()
                    .map(adventure -> new LikedItemDTO(adventure.getId(), adventure.getTitle(), adventure.getDescription()))
                    .toList();
            case LIST -> listRepository.findAllById(ids).stream()
                    .map(list -> new LikedItemDTO(list.getId(), list.getTitle(), list.getDescription()))
                    .toList();
        };
        Map<Long, LikedItemDTO> byId = items.stream()
                .collect(Collectors.toMap(LikedItemDTO::id, Function.identity()));
        return ids.stream().map(byId::get).filter(Objects::nonNull).toList();
    }

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
