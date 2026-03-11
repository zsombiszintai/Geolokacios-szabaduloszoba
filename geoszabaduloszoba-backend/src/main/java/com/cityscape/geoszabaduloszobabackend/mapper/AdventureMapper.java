package com.cityscape.geoszabaduloszobabackend.mapper;

import com.cityscape.geoszabaduloszobabackend.model.dto.AdventureDTO;
import com.cityscape.geoszabaduloszobabackend.model.entity.AdventureEntity;
import org.mapstruct.Mapper;

@Mapper(config = AbstractMapper.class)
public interface AdventureMapper extends AbstractMapper<AdventureEntity, AdventureDTO> {
}

