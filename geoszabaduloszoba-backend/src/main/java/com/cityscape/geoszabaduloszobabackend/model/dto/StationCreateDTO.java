package com.cityscape.geoszabaduloszobabackend.model.dto;

import lombok.Data;

@Data
public class StationCreateDTO {
    private Double latitude;
    private Double longitude;
    private String riddleText;
}
