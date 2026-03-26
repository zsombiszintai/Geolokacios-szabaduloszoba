package com.cityscape.geoszabaduloszobabackend.service;

import com.cityscape.geoszabaduloszobabackend.model.entity.AdventureEntity;
import com.cityscape.geoszabaduloszobabackend.model.entity.StationEntity;
import com.cityscape.geoszabaduloszobabackend.repository.StationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class StationService {
    private final StationRepository stationRepository;

    @Transactional
    public void saveStations(List<StationEntity> stations, AdventureEntity adventure) {
        for (int i = 0; i < stations.size(); i++) {
            StationEntity station = stations.get(i);
            station.setAdventure(adventure);
            station.setSeqNumber(i + 1);
            stationRepository.save(station);
        }
    }

}
