package com.cityscape.geoszabaduloszobabackend.repository;

import com.cityscape.geoszabaduloszobabackend.model.entity.LikeEntity;
import com.cityscape.geoszabaduloszobabackend.model.enums.LikeType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LikeRepository extends JpaRepository<LikeEntity, Long> {
    List<LikeEntity> findAllByLiker_IdAndTypeOrderByLikeDateDescIdDesc(Long likerId, LikeType type);
    boolean existsByLiker_IdAndLikedIdAndType(Long likerId, Long likedId, LikeType type);

    void deleteByLiker_IdAndLikedIdAndType(Long likerId, Long likedId, LikeType type);
}
