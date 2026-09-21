package com.cityscape.geoszabaduloszobabackend.repository;

import com.cityscape.geoszabaduloszobabackend.model.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<UserEntity, Long> {

    @Modifying
    @Query("""
        UPDATE UserEntity u
        SET u.points = COALESCE(u.points, 0) + :points
        WHERE u.id = :userId
        """)
    int addPoints(
            @Param("userId") Long userId,
            @Param("points") int points
    );

    Optional<UserEntity> findByKeycloakSub(String keycloakSub);

    List<UserEntity> findByKeycloakSubIn(List<String> keycloakSubs);
}

