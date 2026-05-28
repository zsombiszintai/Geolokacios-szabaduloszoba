package com.cityscape.geoszabaduloszobabackend.model.dto;

public record AbandonedAdventureDTO(
        Long adventureId,
        String title,
        Long lastStationId,
        Integer lastStationSeq,
        Integer elapsedSec,
        Double distanceTravelled,
        Integer points
) {}
