package com.cityscape.geoszabaduloszobabackend.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StationContent {

    private String riddle;
    private String explanation;
    private List<String> hints;

}
