package com.cityscape.geoszabaduloszobabackend.repository;

import com.cityscape.geoszabaduloszobabackend.model.entity.AiModerationResponseEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface AiModerationResponseRepository
        extends JpaRepository<AiModerationResponseEntity, Long> {

    @Query("""
        select m
        from AiModerationResponseEntity m
        join fetch m.adventure
        where m.adventure.id in :adventureIds
          and m.id = (
              select max(m2.id)
              from AiModerationResponseEntity m2
              where m2.adventure.id = m.adventure.id
          )
        """)
    List<AiModerationResponseEntity> findLatestByAdventureIds(
            @Param("adventureIds") Collection<Long> adventureIds
    );

    void deleteByAdventureId(Long adventureId);
}