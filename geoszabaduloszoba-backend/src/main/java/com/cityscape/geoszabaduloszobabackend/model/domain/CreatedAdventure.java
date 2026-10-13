package com.cityscape.geoszabaduloszobabackend.model.domain;
import com.cityscape.geoszabaduloszobabackend.model.entity.*;
import java.util.List;
public record CreatedAdventure(AdventureEntity adventure, AiModerationResponseEntity moderation) {}
