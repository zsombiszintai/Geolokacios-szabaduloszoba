package com.cityscape.geoszabaduloszobabackend.api;

import com.cityscape.geoszabaduloszobabackend.model.dto.AdventureListDTO;
import com.cityscape.geoszabaduloszobabackend.model.view.UserAdventureStatistics;
import com.cityscape.geoszabaduloszobabackend.service.ProfileService;
import com.cityscape.geoszabaduloszobabackend.api.mapper.ApiMapper;
import com.cityscape.geoszabaduloszobabackend.model.domain.*;
import com.cityscape.geoszabaduloszobabackend.model.entity.*;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/profile")
@RequiredArgsConstructor
public class ProfileAPI {

    private final ProfileService profileService;
    private final ApiMapper apiMapper;

    @GetMapping("/me")
    public UserAdventureStatistics getMyProfile() {
        return profileService.getMyStats();
    }

    @GetMapping("/user/{username}")
    public UserAdventureStatistics getUserProfile(@PathVariable String username) {
        return profileService.getUserStats(username);
    }

    @GetMapping("/list/{type}")
    public List<?> getList(@PathVariable String type, @RequestParam(required = false) String username) {
        return profileService.getListByType(username, type).stream()
                .map(this::toDto).toList();
    }
    private Object toDto(Object result) {
        if (result instanceof AdventureEntity adventure) return apiMapper.toDto(adventure);
        if (result instanceof ReviewEntity review) return apiMapper.toDto(review);
        if (result instanceof UserSummary user) return apiMapper.toDto(user);
        if (result instanceof ReviewedAdventure review) return apiMapper.toDto(review);
        if (result == null) return null;
        throw new IllegalArgumentException("Unsupported profile list result: " + result.getClass());
    }
}
