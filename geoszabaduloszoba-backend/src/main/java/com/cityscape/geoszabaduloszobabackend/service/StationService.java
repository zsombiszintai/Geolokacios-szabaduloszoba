package com.cityscape.geoszabaduloszobabackend.service;

import com.cityscape.geoszabaduloszobabackend.model.dto.StationContent;
import com.cityscape.geoszabaduloszobabackend.model.dto.StationDTO;
import com.cityscape.geoszabaduloszobabackend.model.entity.AdventureEntity;
import com.cityscape.geoszabaduloszobabackend.model.entity.StationEntity;
import com.cityscape.geoszabaduloszobabackend.repository.StationRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class StationService {

    private final StationRepository stationRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Transactional
    public void saveStations(List<StationEntity> stations, AdventureEntity adventure) {
        for (int i = 0; i < stations.size(); i++) {
            StationEntity station = stations.get(i);
            station.setAdventure(adventure);
            station.setSeqNumber(i + 1);
            station.setLastStation(i == stations.size() - 1);
            stationRepository.save(station);
        }
    }

    public StationDTO convertToDTO(StationEntity entity) {
        StationContent content = null;
        try {
            if (entity.getContent() != null) {
                content = objectMapper.readValue(entity.getContent(), StationContent.class);
            }
        } catch (Exception e) {
            content = new StationContent("Hiba a tartalom betöltésekor", "", List.of());
        }

        return new StationDTO(
                entity.getId(),
                entity.getLatitude(),
                entity.getLongitude(),
                content,
                entity.getSeqNumber()
        );
    }

}
