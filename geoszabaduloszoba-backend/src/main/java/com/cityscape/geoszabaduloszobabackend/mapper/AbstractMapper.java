package com.cityscape.geoszabaduloszobabackend.mapper;

import org.mapstruct.MapperConfig;
import org.mapstruct.ReportingPolicy;

@MapperConfig(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface AbstractMapper<Entity, DTO> {

    public DTO mapDTO(Entity entity);

    public Entity mapEntity(DTO dto);
}
