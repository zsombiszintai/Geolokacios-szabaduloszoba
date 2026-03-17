package com.cityscape.geoszabaduloszobabackend.repository;

import com.cityscape.geoszabaduloszobabackend.model.entity.StationEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface StationRepository extends JpaRepository<StationEntity, Long> {

    Optional<StationEntity> findByAdventureIdAndSeqNumber(Long adventureId, Integer seqNumber);

}
