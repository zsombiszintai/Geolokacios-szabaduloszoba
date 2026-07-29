package com.cityscape.geoszabaduloszobabackend.repository;

import com.cityscape.geoszabaduloszobabackend.model.entity.StationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StationRepository extends JpaRepository<StationEntity, Long> {

    Optional<StationEntity> findByAdventureIdAndSeqNumber(Long adventureId, Integer seqNumber);

    List<StationEntity> findAllByAdventureIdOrderBySeqNumberAsc(Long adventureId);

    void deleteByAdventureId(Long id);

    List<StationEntity> findByAdventureIdOrderBySeqNumberAsc(Long id);
}
