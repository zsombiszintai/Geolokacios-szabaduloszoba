package com.cityscape.geoszabaduloszobabackend.repository;

import com.cityscape.geoszabaduloszobabackend.model.view.UserAdventureStatistics;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserAdventureStatisticsRepository extends JpaRepository<UserAdventureStatistics, Long> {
    Optional<UserAdventureStatistics> findByKeycloakSub(String keycloakSub);
    Optional<UserAdventureStatistics> findByUsername (String username);
}
