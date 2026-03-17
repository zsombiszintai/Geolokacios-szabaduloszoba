package com.cityscape.geoszabaduloszobabackend.model.dto;

public record NearbyAdventureDTO(

        Long id,

        String title,

        Integer distanceInMeters,

        String averageTime
) {}