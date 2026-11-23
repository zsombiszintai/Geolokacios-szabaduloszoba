package com.cityscape.geoszabaduloszobabackend.repository;

import com.cityscape.geoszabaduloszobabackend.model.entity.LikeEntity;
import com.cityscape.geoszabaduloszobabackend.model.enums.LikeType;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LikeRepository extends JpaRepository<LikeEntity, Long> {
    boolean existsByLiker_IdAndLikedIdAndType(Long likerId, Long likedId, LikeType type);

    void deleteByLiker_IdAndLikedIdAndType(Long likerId, Long likedId, LikeType type);
}
