package com.cityscape.geoszabaduloszobabackend.model.dto;

public record NearbyAdventureDTO(

        Long id,
        String title,
        Integer distanceInMeters,
        Integer averageTime,
        Double advLon,
        Double advLat
) {}