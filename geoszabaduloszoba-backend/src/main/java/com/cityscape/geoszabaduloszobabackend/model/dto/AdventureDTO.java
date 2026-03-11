package com.cityscape.geoszabaduloszobabackend.model.dto;

import lombok.Data;

import java.util.List;

@Data
public class AdventureDTO {
    private Long id;
    private String name;
    private String averageTime;
    private Integer distanceInMeters;
    private String difficulty; // pl. "Nehéz"
    private String creatorName;

    private Double averageRating;
    private List<Integer> ratingDistribution;
}
