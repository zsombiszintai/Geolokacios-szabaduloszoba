package com.cityscape.geoszabaduloszobabackend.repository;

import com.cityscape.geoszabaduloszobabackend.model.entity.AdventureEntity;
import com.cityscape.geoszabaduloszobabackend.model.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface AdventureRepository extends JpaRepository<AdventureEntity, Long> {

    List<AdventureEntity> findByTitleContainingIgnoreCase(String query);

    List<AdventureEntity> findAllByCreator(UserEntity creator);

    List<AdventureEntity> findAllByCreatorKeycloakSub(String sub);

    List<AdventureEntity> findByStatus(String status);
}
