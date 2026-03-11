package com.cityscape.geoszabaduloszobabackend.api;

import com.cityscape.geoszabaduloszobabackend.mapper.AdventureMapper;
import com.cityscape.geoszabaduloszobabackend.model.dto.AdventureDTO;
import com.cityscape.geoszabaduloszobabackend.model.entity.AdventureEntity;
import com.cityscape.geoszabaduloszobabackend.service.AdventureService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/adventures")
public class AdventureAPI extends AbstractCrudAPI<AdventureDTO, AdventureEntity> {

    public AdventureAPI(AdventureMapper mapper, AdventureService service) {
        super(mapper, service);
    }

    // Dashboard specifikus végpont: Kalandok keresése koordináta alapján
    @GetMapping("/nearby")
    public List<AdventureDTO> getNearbyAdventures(
            @RequestParam double lat,
            @RequestParam double lon,
            @RequestParam(defaultValue = "5000") double radiusInMeters) {

        return ((AdventureService) service).findNearby(lat, lon, radiusInMeters)
                .stream()
                .map(mapper::mapDTO)
                .toList();
    }
}
