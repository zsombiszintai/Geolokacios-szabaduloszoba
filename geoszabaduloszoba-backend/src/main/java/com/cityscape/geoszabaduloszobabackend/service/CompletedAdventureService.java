package com.cityscape.geoszabaduloszobabackend.service;

import com.cityscape.geoszabaduloszobabackend.model.domain.CompletedAdventure;
import com.cityscape.geoszabaduloszobabackend.model.entity.CompletedAdventureEntity;
import com.cityscape.geoszabaduloszobabackend.model.entity.ReviewEntity;
import com.cityscape.geoszabaduloszobabackend.model.entity.UserEntity;
import com.cityscape.geoszabaduloszobabackend.repository.CompletedAdventureRepository;
import com.cityscape.geoszabaduloszobabackend.repository.ReviewRepository;
import com.cityscape.geoszabaduloszobabackend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class CompletedAdventureService {
    private final UserService userService;

    private final CompletedAdventureRepository completedRepository;
    private final ReviewRepository reviewRepository;
    private final UserRepository userRepository;

    public List<CompletedAdventure> getUserCompletedAdventures() {
        String keycloakSub = userService.getCurrentUserId();
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

                    return new CompletedAdventure(completed, review);
                })
                .toList();
    }

}
