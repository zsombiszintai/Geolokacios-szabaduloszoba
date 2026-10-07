package com.cityscape.geoszabaduloszobabackend.model.dto;

import java.time.LocalDate;

public record AdventureCreatedDTO(
        Long id,
        String title,
        LocalDate createdAt,
        String status,
        AiModerationResponse aiModeration
) {}