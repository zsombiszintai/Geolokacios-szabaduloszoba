package com.cityscape.geoszabaduloszobabackend.model.dto;
import java.util.List;
public record AdventureCollectionDTO(Long id, String title, String description, List<Long> adventureIds) {}
