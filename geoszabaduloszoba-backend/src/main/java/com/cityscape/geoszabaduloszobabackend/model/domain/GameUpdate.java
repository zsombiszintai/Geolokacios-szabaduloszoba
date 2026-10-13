package com.cityscape.geoszabaduloszobabackend.model.domain;

public record GameUpdate(
        Long sessionId,
        Long lastStationId,
        String title,
        Integer elapsedSec,
        Double distanceInMeters,
        Integer points
) {}
