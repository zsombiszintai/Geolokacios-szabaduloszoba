package com.cityscape.geoszabaduloszobabackend.model.dto;

public record ActiveGameDTO(
        Long sessionId,
        Long lastStationId,
        String title,
        Integer elapsedSec,
        Double distanceInMeters,
        Double currentLat,
        Double currentLon
) {}
