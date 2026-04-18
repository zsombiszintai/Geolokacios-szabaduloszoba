package com.cityscape.geoszabaduloszobabackend.service;

import com.cityscape.geoszabaduloszobabackend.model.view.UserAdventureStatistics;
import com.cityscape.geoszabaduloszobabackend.repository.UserAdventureStatisticsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProfileService {

    private final UserAdventureStatisticsRepository statsRepository;

    @Transactional(readOnly = true)
    public UserAdventureStatistics getMyStats(String keycloakSub) {
        return statsRepository.findByKeycloakSub(keycloakSub)
                .orElseThrow(() -> new RuntimeException("User not found:" + keycloakSub));
    }

    @Transactional(readOnly = true)
    public UserAdventureStatistics getUserStats(String username) {
        return statsRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found:" + username));
    }
}
