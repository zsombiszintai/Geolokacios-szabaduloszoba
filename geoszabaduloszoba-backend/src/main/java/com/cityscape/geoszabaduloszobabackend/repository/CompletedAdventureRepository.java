package com.cityscape.geoszabaduloszobabackend.repository;

import com.cityscape.geoszabaduloszobabackend.model.entity.CompletedAdventureEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CompletedAdventureRepository extends JpaRepository<CompletedAdventureEntity, Long> {

    List<CompletedAdventureEntity> findCompletedAdventureEntitiesByUserId(Long userId);
}
