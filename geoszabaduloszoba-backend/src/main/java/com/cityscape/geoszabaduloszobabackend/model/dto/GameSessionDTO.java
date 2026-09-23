package com.cityscape.geoszabaduloszobabackend.model.dto;

public record GameSessionDTO(
        Long sessionId,
        Long adventureId,
        Long lastStationId,
        Integer elapsedSec,
        Double distanceInMeters,
        Integer points,
        boolean completed
) {}