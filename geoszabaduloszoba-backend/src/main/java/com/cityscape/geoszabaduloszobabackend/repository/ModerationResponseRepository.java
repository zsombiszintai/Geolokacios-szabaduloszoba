package com.cityscape.geoszabaduloszobabackend.repository;

import com.cityscape.geoszabaduloszobabackend.model.entity.ModerationResponseEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ModerationResponseRepository extends JpaRepository<ModerationResponseEntity, Long> {

    Optional<ModerationResponseEntity> findTopByAdventureIdOrderByCreatedAtDesc(Long adventureId);
}
