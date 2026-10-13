package com.cityscape.geoszabaduloszobabackend.api;

import com.cityscape.geoszabaduloszobabackend.api.mapper.AdventureMapper;
import com.cityscape.geoszabaduloszobabackend.model.dto.AdventureCreateDTO;
import com.cityscape.geoszabaduloszobabackend.model.dto.AdventureCreatedDTO;
import com.cityscape.geoszabaduloszobabackend.model.entity.AdventureEntity;
import com.cityscape.geoszabaduloszobabackend.model.entity.StationEntity;
import com.cityscape.geoszabaduloszobabackend.service.AdventureService;
import com.cityscape.geoszabaduloszobabackend.api.mapper.ApiMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/create-adventure")
@RequiredArgsConstructor
public class CreateAdventureAPI {

    private final ApiMapper apiMapper;

    private final AdventureService adventureService;
    private final AdventureMapper adventureMapper;

    @PostMapping
    public Long create(@RequestBody AdventureCreateDTO dto) {

        AdventureEntity adventure = adventureMapper.toEntity(dto);

        if (dto.getStatus() != null) {
            adventure.setStatus(dto.getStatus());
        } else {
            adventure.setStatus("DRAFT");
        }

        List<StationEntity> stations = adventureMapper.toStationEntities(dto.getStations());
        return adventureService.createAdventureWithStations(adventure, stations).getId();
    }

    @GetMapping("/created-adventures")
    public List<AdventureCreatedDTO> getMyAdventures() {
        return adventureService.getMyAdventures().stream().map(apiMapper::toDto).toList();
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        adventureService.deleteAdventure(id);
    }

    @GetMapping("/{id}")
    public AdventureCreateDTO getForEdit(@PathVariable Long id) {
        return apiMapper.toDto(adventureService.getAdventureForEdit(id));
    }

    @PutMapping("/{id}")
    public void update(@PathVariable Long id, @RequestBody AdventureCreateDTO dto) {
        AdventureEntity updatedAdventure = adventureMapper.toEntity(dto);

        if (dto.getStatus() != null) {
            updatedAdventure.setStatus(dto.getStatus());
        }

        List<StationEntity> stations = adventureMapper.toStationEntities(dto.getStations());
        adventureService.updateAdventureWithStations(id, updatedAdventure, stations);
    }
}
