package com.cityscape.geoszabaduloszobabackend.model.dto;

import java.time.LocalDate;

public record ReviewDTO(
        Long id,
        Long adventureId,
        String title,
        Integer rating,
        String reviewText,
        LocalDate reviewedAt
) {}
