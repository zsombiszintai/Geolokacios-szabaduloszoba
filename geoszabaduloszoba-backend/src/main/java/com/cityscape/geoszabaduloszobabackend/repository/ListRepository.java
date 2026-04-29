package com.cityscape.geoszabaduloszobabackend.repository;

import com.cityscape.geoszabaduloszobabackend.model.entity.ListEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface ListRepository extends JpaRepository<ListEntity, Long> {

    List<ListEntity> findAllByCreatorKeycloakSub(String keycloakSub);

    List<ListEntity> findByTitleContainingIgnoreCase(String title);
}
