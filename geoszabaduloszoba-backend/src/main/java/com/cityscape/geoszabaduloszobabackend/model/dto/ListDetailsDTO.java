package com.cityscape.geoszabaduloszobabackend.model.dto;

import java.util.List;

public record ListDetailsDTO(
        Long id,
        String title,
        String description,
        List<AdventureListDTO> adventures
) {}