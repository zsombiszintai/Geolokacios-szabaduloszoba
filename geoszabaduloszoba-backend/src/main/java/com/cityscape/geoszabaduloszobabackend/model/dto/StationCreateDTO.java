package com.cityscape.geoszabaduloszobabackend.model.dto;

import lombok.Data;

public record StationCreateDTO(
        Integer seqNumber,
        StationContent content,
        Double latitude,
        Double longitude,
        Boolean isLastStation
) {}
