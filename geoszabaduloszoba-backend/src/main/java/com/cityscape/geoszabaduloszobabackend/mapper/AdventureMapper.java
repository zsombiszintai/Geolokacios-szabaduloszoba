package com.cityscape.geoszabaduloszobabackend.mapper;

import com.cityscape.geoszabaduloszobabackend.model.dto.AdventureCreateDTO;
import com.cityscape.geoszabaduloszobabackend.model.dto.AdventureProfileDTO;
import com.cityscape.geoszabaduloszobabackend.model.dto.StationContent;
import com.cityscape.geoszabaduloszobabackend.model.dto.StationCreateDTO;
import com.cityscape.geoszabaduloszobabackend.model.entity.AdventureEntity;
import com.cityscape.geoszabaduloszobabackend.model.entity.StationEntity;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

@Mapper(componentModel = "spring")
public abstract class AdventureMapper {

    @Autowired
    protected ObjectMapper objectMapper;

    @Mapping(target = "creator", ignore = true)
    @Mapping(target = "totalDistance", ignore = true)
    public abstract AdventureEntity toEntity(AdventureCreateDTO dto);

    @Mapping(target = "content", source = "content", qualifiedByName = "contentToJson")
    public abstract StationEntity toStationEntity(StationCreateDTO dto);

    public abstract List<StationEntity> toStationEntities(List<StationCreateDTO> dtos);

    @Named("contentToJson")
    protected String contentToJson(StationContent content) {
        try {
            return content != null ? objectMapper.writeValueAsString(content) : null;
        } catch (JsonProcessingException e) {
            return null;
        }
    }
}

