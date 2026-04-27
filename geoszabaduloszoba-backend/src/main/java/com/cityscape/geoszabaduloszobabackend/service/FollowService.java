package com.cityscape.geoszabaduloszobabackend.service;

import com.cityscape.geoszabaduloszobabackend.model.entity.FollowEntity;
import com.cityscape.geoszabaduloszobabackend.model.entity.UserEntity;
import com.cityscape.geoszabaduloszobabackend.repository.FollowRepository;
import com.cityscape.geoszabaduloszobabackend.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class FollowService {

    private final FollowRepository followRepository;
    private final UserRepository userRepository;

    @Transactional
    public void followUser(String followerSub, Long followedId) {
        UserEntity follower = userRepository.findByKeycloakSub(followerSub)
                .orElseThrow(() -> new RuntimeException("Follower not found"));
        UserEntity followed = userRepository.findById(followedId)
                .orElseThrow(() -> new RuntimeException("Followed user not found"));

        if (follower.getId().equals(followedId)) {
            throw new RuntimeException("Nem követheted saját magadat!");
        }

        if (!followRepository.existsByFollowerAndFollowed(follower, followed)) {
            FollowEntity follow = FollowEntity.builder()
                    .follower(follower)
                    .followed(followed)
                    .createdAt(LocalDateTime.now())
                    .build();
            followRepository.save(follow);
        }
    }

    @Transactional
    public void unfollowUser(String followerSub, Long followedId) {
        UserEntity follower = userRepository.findByKeycloakSub(followerSub)
                .orElseThrow(() -> new RuntimeException("Follower not found"));
        UserEntity followed = userRepository.findById(followedId)
                .orElseThrow(() -> new RuntimeException("Followed user not found"));

        followRepository.deleteByFollowerAndFollowed(follower, followed);
    }

    public boolean isFollowing(String followerSub, Long followedId) {
        return userRepository.findByKeycloakSub(followerSub)
                .map(follower -> {
                    UserEntity followed = userRepository.findById(followedId).orElse(null);
                    return followed != null && followRepository.existsByFollowerAndFollowed(follower, followed);
                }).orElse(false);
    }
}
