package com.cityscape.geoszabaduloszobabackend.model.domain;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SearchResult {

    private Long id;
    private String title;
    private String creator;
    private String type;
    private Double lat;
    private Double lon;
}
