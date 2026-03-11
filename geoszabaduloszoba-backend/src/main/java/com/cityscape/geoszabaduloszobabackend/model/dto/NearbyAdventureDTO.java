package com.cityscape.geoszabaduloszobabackend.model.dto;

public record NearbyAdventureDTO(
        Long id,
        String name,
        Integer distanceInMeters,
        String averageTime
) {}
