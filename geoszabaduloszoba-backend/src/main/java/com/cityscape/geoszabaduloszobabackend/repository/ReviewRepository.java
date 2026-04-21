package com.cityscape.geoszabaduloszobabackend.repository;

import com.cityscape.geoszabaduloszobabackend.model.entity.ReviewEntity;
import com.cityscape.geoszabaduloszobabackend.model.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface ReviewRepository extends JpaRepository<ReviewEntity, Long> {

    List<ReviewEntity> findAllByUserKeycloakSubAndRatingIsNotNull(String sub);

    List<ReviewEntity> findByUserOrderByReviewedAtDesc(UserEntity user);

    List<ReviewEntity> findAllByUserKeycloakSub(String sub);
}