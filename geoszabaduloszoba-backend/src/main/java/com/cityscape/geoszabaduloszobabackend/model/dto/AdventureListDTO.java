package com.cityscape.geoszabaduloszobabackend.model.dto;

import com.cityscape.geoszabaduloszobabackend.model.enums.Difficulty;

public record AdventureListDTO(
        Long id,
        String title,
        String description,
        Difficulty difficulty
) {}