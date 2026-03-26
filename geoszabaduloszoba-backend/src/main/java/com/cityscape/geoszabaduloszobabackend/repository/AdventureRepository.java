package com.cityscape.geoszabaduloszobabackend.repository;

import com.cityscape.geoszabaduloszobabackend.model.entity.AdventureEntity;
import com.cityscape.geoszabaduloszobabackend.model.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface AdventureRepository extends JpaRepository<AdventureEntity, Long> {

    List<AdventureEntity> findByTitleContainingIgnoreCase(String query);

    List<AdventureEntity> findAllByCreator(UserEntity creator);
}
