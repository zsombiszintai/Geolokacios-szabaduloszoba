package com.cityscape.geoszabaduloszobabackend.model.domain;

import com.cityscape.geoszabaduloszobabackend.model.enums.Difficulty;

public record AdventureSearchFilters(Difficulty difficulty, Double minRating, Double maxDistanceMeters,
                                     Integer maxDurationSeconds) {
    public static final AdventureSearchFilters NONE = new AdventureSearchFilters(null, null, null, null);
}
