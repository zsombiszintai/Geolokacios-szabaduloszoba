package com.cityscape.geoszabaduloszobabackend.repository;

import com.cityscape.geoszabaduloszobabackend.model.entity.FollowEntity;
import com.cityscape.geoszabaduloszobabackend.model.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FollowRepository extends JpaRepository<FollowEntity, Long> {

    boolean existsByFollowerAndFollowed(UserEntity follower, UserEntity followed);

    void deleteByFollowerAndFollowed(UserEntity follower, UserEntity followed);

    List<FollowEntity> findAllByFollowedKeycloakSub(String sub);

    List<FollowEntity> findAllByFollowerKeycloakSub(String sub);
}
