package com.cityscape.geoszabaduloszobabackend.model.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SearchDTO {

    private Long id;
    private String title;
    private String creator;
    private String type;
    private Double lat;
    private Double lon;
}
