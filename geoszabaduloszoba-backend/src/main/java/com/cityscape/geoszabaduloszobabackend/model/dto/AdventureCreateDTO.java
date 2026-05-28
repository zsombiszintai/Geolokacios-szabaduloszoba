package com.cityscape.geoszabaduloszobabackend.model.dto;

import lombok.Data;
import java.util.List;

@Data
public class AdventureCreateDTO {
    private String title;
    private String description;
    private String difficulty;
    private List<StationCreateDTO> stations;
}
