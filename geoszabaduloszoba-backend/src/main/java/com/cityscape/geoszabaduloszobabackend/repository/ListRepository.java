package com.cityscape.geoszabaduloszobabackend.repository;

import com.cityscape.geoszabaduloszobabackend.model.entity.ListEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import java.util.Optional;

import java.util.Collection;
import java.util.List;

@Repository
public interface ListRepository extends JpaRepository<ListEntity, Long> {

    @EntityGraph(attributePaths = "adventures")
    List<ListEntity> findAllByCreatorKeycloakSub(String keycloakSub);

    @Override
    @EntityGraph(attributePaths = "adventures")
    Optional<ListEntity> findById(Long id);

    List<ListEntity> findByTitleContainingIgnoreCase(String title);

    Page<ListEntity> findByTitleContainingIgnoreCase(String title, Pageable pageable);
}
