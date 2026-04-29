package com.cityscape.geoszabaduloszobabackend.repository;

import com.cityscape.geoszabaduloszobabackend.model.entity.AbandonedAdventureEntity;
import com.cityscape.geoszabaduloszobabackend.model.entity.CompletedAdventureEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AbandonedAdventureRepository extends JpaRepository<AbandonedAdventureEntity, Long> {

    List<AbandonedAdventureEntity> findAllByUserKeycloakSub(String sub);

    Optional<AbandonedAdventureEntity> findFirstByUserKeycloakSubAndIsCompletedFalseOrderByStartedAtDesc(String sub);
}
