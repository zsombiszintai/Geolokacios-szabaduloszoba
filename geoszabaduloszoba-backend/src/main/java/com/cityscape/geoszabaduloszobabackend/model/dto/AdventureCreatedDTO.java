package com.cityscape.geoszabaduloszobabackend.model.dto;

import com.cityscape.geoszabaduloszobabackend.model.enums.AdventureStatus;
import com.fasterxml.jackson.annotation.JsonFormat;
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

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate createdAt;
    private String status;
}
