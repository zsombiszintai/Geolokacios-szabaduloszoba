package com.cityscape.geoszabaduloszobabackend.repository;

import com.cityscape.geoszabaduloszobabackend.model.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<UserEntity, Long> {

    Optional<UserEntity> findByKeycloakSub(String keycloakSub);

    Optional<UserEntity> findByUsername(String username);

    boolean existsByUsername(String username);

    Optional<UserEntity> findByEmail(String email);
}

