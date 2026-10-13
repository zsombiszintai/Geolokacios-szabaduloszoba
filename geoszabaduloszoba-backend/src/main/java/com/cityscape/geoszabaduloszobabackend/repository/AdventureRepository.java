package com.cityscape.geoszabaduloszobabackend.repository;

import com.cityscape.geoszabaduloszobabackend.model.entity.AdventureEntity;
import com.cityscape.geoszabaduloszobabackend.model.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

@Repository
public interface AdventureRepository extends JpaRepository<AdventureEntity, Long> {

    List<AdventureEntity> findByTitleContainingIgnoreCase(String query);

    List<AdventureEntity> findAllByCreator(UserEntity creator);

    List<AdventureEntity> findAllByCreatorKeycloakSub(String sub);

    List<AdventureEntity> findByStatus(String status);

    List<AdventureEntity> findTop3ByStatusOrderByIdAsc(String pending);
    @Query("""
        select adventure from AdventureEntity adventure
        where adventure.status = 'PUBLIC'
          and (:query = '' or lower(adventure.title) like lower(concat('%', :query, '%')) escape '\\')
          and exists (
              select station.id from StationEntity station
              where station.adventure = adventure
                and station.latitude is not null and station.longitude is not null
                and (station.seqNumber = 0 or (
                    station.seqNumber = 1 and not exists (
                        select start.id from StationEntity start
                        where start.adventure = adventure and start.seqNumber = 0
                    )
                ))
          )
        """)
    Page<AdventureEntity> searchPublicWithLocation(@Param("query") String query, Pageable pageable);
}
