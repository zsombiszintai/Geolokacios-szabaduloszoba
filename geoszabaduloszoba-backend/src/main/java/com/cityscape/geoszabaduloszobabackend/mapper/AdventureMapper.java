package com.cityscape.geoszabaduloszobabackend.mapper;

import com.cityscape.geoszabaduloszobabackend.model.dto.AdventureCreateDTO;
import com.cityscape.geoszabaduloszobabackend.model.dto.AdventureProfileDTO;
import com.cityscape.geoszabaduloszobabackend.model.dto.StationCreateDTO;
import com.cityscape.geoszabaduloszobabackend.model.entity.AdventureEntity;
import com.cityscape.geoszabaduloszobabackend.model.entity.StationEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface AdventureMapper{

    @Mapping(target = "creator", ignore = true)
    @Mapping(target = "totalDistance", ignore = true)
    AdventureEntity toEntity(AdventureCreateDTO dto);

    AdventureProfileDTO toProfileDTO(AdventureEntity entity);

    StationEntity toStationEntity(StationCreateDTO dto);
    List<StationEntity> toStationEntities(List<StationCreateDTO> dtos);
}

