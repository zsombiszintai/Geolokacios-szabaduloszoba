package com.cityscape.geoszabaduloszobabackend.model.dto;

import com.cityscape.geoszabaduloszobabackend.model.enums.AdventureStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AdventureCreatedDTO {
    private Long id;
    private String title;
    private LocalDate createdAt;
    private String status;
}
