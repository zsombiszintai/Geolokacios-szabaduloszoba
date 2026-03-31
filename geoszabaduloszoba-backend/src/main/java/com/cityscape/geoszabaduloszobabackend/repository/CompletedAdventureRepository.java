package com.cityscape.geoszabaduloszobabackend.repository;

import com.cityscape.geoszabaduloszobabackend.model.entity.AbandonedAdventureEntity;
import com.cityscape.geoszabaduloszobabackend.model.entity.CompletedAdventureEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CompletedAdventureRepository extends JpaRepository<CompletedAdventureEntity, Long> {
}
