package com.cityscape.geoszabaduloszobabackend.model.dto;

public record StationCreateDTO(
        Integer seqNumber,
        StationContent content,
        Double latitude,
        Double longitude,
        Boolean isLastStation
) {}
