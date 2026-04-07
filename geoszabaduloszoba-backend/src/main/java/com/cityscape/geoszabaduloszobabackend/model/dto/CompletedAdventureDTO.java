package com.cityscape.geoszabaduloszobabackend.model.dto;

import java.time.LocalDate;

public record CompletedAdventureDTO(
        Long id,
        Long adventureId,
        String adventureTitle,
        LocalDate completedAt,
        Double distanceTravelled,
        Integer durationSec,
        Integer rating
) {}