package com.cityscape.geoszabaduloszobabackend.model.dto;

public record AbandonedAdventureDTO(
        Long adventureId,
        String title,
        Long lastStationId,
        Integer elapsedSec,
        Double distanceTravelled
) {}
