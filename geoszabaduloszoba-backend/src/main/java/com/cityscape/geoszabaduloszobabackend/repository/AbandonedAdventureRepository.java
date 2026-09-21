package com.cityscape.geoszabaduloszobabackend.repository;

import com.cityscape.geoszabaduloszobabackend.model.entity.AbandonedAdventureEntity;
import com.cityscape.geoszabaduloszobabackend.model.entity.CompletedAdventureEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AbandonedAdventureRepository extends JpaRepository<AbandonedAdventureEntity, Long> {

    @Query(
            value = "SELECT id FROM abandoned_adventure WHERE id = :id FOR UPDATE",
            nativeQuery = true
    )
    Optional<Long> lockSession(@Param("id") Long id);

    List<AbandonedAdventureEntity> findAllByUserKeycloakSub(String sub);

}
