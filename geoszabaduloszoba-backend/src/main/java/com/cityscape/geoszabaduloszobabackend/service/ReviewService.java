package com.cityscape.geoszabaduloszobabackend.service;
import com.cityscape.geoszabaduloszobabackend.model.entity.ReviewEntity;
import com.cityscape.geoszabaduloszobabackend.repository.AdventureRepository;
import com.cityscape.geoszabaduloszobabackend.repository.ReviewRepository;
import com.cityscape.geoszabaduloszobabackend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
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
    private final UserService userService;

    public void saveReview(Long adventureId, ReviewEntity review) {
        var user = userRepository.findByKeycloakSub(userService.getCurrentUserId())
                .orElseThrow(() -> new RuntimeException("Felhasználó nem található"));
        var adventure = adventureRepository.findById(adventureId)
                .orElseThrow(() -> new RuntimeException("Kaland nem található"));
        review.setId(null);
        review.setUser(user);
        review.setAdventure(adventure);
        review.setReviewedAt(LocalDate.now());
        reviewRepository.save(review);
    }

    public List<ReviewEntity> getUserReviews() {
        var user = userRepository.findByKeycloakSub(userService.getCurrentUserId())
                .orElseThrow(() -> new RuntimeException("Felhasználó nem található"));
        return reviewRepository.findByUserOrderByReviewedAtDesc(user);
    }
}
