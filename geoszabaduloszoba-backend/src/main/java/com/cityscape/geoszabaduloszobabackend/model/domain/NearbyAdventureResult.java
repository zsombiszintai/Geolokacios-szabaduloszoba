package com.cityscape.geoszabaduloszobabackend.model.domain;

public record NearbyAdventureResult(

        Long id,
        String title,
        Integer distanceInMeters,
        Integer averageTime,
        Double advLon,
        Double advLat
) {}