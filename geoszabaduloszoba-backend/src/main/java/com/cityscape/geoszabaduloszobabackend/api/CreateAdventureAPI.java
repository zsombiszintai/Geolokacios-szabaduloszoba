package com.cityscape.geoszabaduloszobabackend.api;

import com.cityscape.geoszabaduloszobabackend.mapper.AdventureMapper;
import com.cityscape.geoszabaduloszobabackend.model.dto.AdventureCreateDTO;
import com.cityscape.geoszabaduloszobabackend.model.dto.AdventureCreatedDTO;
import com.cityscape.geoszabaduloszobabackend.model.entity.AdventureEntity;
import com.cityscape.geoszabaduloszobabackend.model.entity.StationEntity;
import com.cityscape.geoszabaduloszobabackend.model.entity.UserEntity;
import com.cityscape.geoszabaduloszobabackend.service.AdventureService;
import com.cityscape.geoszabaduloszobabackend.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/create-adventure")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:5173")
public class CreateAdventureAPI {

    private final AdventureService adventureService;
    private final AdventureMapper adventureMapper;
    private final UserService userService;

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
        UserEntity currentUser = userService.getOrCreateCurrentUser();
        return adventureService.getAdventuresByUser(currentUser);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        adventureService.deleteAdventure(id);
    }

    @GetMapping("/{id}")
    public AdventureCreateDTO getForEdit(@PathVariable Long id) {
        return adventureService.getAdventureForEdit(id);
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
