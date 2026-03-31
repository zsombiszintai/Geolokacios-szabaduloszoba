package com.cityscape.geoszabaduloszobabackend.repository;

import com.cityscape.geoszabaduloszobabackend.model.entity.AbandonedAdventureEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AbandonedAdventureRepository extends JpaRepository<AbandonedAdventureEntity, Long> {
}
