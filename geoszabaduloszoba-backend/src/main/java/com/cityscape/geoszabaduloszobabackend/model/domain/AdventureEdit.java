package com.cityscape.geoszabaduloszobabackend.model.domain;
import com.cityscape.geoszabaduloszobabackend.model.entity.*;
import java.util.List;
public record AdventureEdit(AdventureEntity adventure, List<StationEntity> stations) {}
