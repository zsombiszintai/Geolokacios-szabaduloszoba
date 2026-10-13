package com.cityscape.geoszabaduloszobabackend.model.domain;
import com.cityscape.geoszabaduloszobabackend.model.entity.*;
import java.util.List;
public record AdventureDetails(AdventureEntity adventure, List<StationEntity> stations, List<ReviewEntity> reviews, String creatorName, String creatorAvatarUrl) {}
