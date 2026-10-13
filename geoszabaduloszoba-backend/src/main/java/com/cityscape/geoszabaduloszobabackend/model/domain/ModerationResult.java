package com.cityscape.geoszabaduloszobabackend.model.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ModerationResult {

    @JsonProperty("isProfane")
    private boolean isProfane;

    @JsonProperty("isSolvable")
    private boolean isSolvable;

    private String profanityDetails;
    private String solvabilityDetails;
    private boolean overallApproved;
    private String reason;
}