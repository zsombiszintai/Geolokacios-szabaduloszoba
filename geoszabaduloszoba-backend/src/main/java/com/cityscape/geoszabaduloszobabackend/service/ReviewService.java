package com.cityscape.geoszabaduloszobabackend.service;
import com.cityscape.geoszabaduloszobabackend.model.entity.ReviewEntity;
import com.cityscape.geoszabaduloszobabackend.repository.AdventureRepository;
import com.cityscape.geoszabaduloszobabackend.repository.ReviewRepository;
import com.cityscape.geoszabaduloszobabackend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
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
        if (review.getRating() != null && (review.getRating() < 1 || review.getRating() > 5)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Az értékelés 1 és 5 közötti lehet.");
        }
        if (review.getReviewText() != null && review.getReviewText().length() > 500) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A vélemény legfeljebb 500 karakter lehet.");
        }
        var user = userRepository.findByKeycloakSub(userService.getCurrentUserId())
                .orElseThrow(() -> new RuntimeException("Felhasználó nem található"));
        var adventure = adventureRepository.findByIdForUpdate(adventureId)
                .orElseThrow(() -> new RuntimeException("Kaland nem található"));
        var previous = reviewRepository.findByAdventureIdAndUserIdOrderByIdDesc(adventureId, user.getId());
        var saved = previous.isEmpty() ? new ReviewEntity() : previous.get(0);
        // Repair older duplicates belonging to this active account only.
        if (previous.size() > 1) reviewRepository.deleteAll(previous.subList(1, previous.size()));
        saved.setUser(user);
        saved.setAdventure(adventure);
        if (review.getRating() != null) saved.setRating(review.getRating());
        if (review.getReviewText() != null) saved.setReviewText(review.getReviewText());
        saved.setReviewedAt(LocalDate.now());
        reviewRepository.save(saved);
    }

    public List<ReviewEntity> getUserReviews() {
        var user = userRepository.findByKeycloakSub(userService.getCurrentUserId())
                .orElseThrow(() -> new RuntimeException("Felhasználó nem található"));
        return reviewRepository.findByUserOrderByReviewedAtDesc(user);
    }
}
