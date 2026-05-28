package com.cityscape.geoszabaduloszobabackend.model.dto;

public record StationDTO(
        Long id,
        Double latitude,
        Double longitude,
        StationContent content,
        Integer seqNumber
) {}
