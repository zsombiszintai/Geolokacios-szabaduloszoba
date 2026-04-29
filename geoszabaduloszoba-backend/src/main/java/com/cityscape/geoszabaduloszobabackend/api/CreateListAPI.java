package com.cityscape.geoszabaduloszobabackend.api;

import com.cityscape.geoszabaduloszobabackend.model.entity.ListEntity;
import com.cityscape.geoszabaduloszobabackend.service.ListService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/lists")
@RequiredArgsConstructor
public class CreateListAPI {

    private final ListService listService;

    @GetMapping
    public List<ListEntity> getMyLists(@AuthenticationPrincipal Jwt jwt) {
        return listService.getMyLists(jwt.getSubject());
    }

    @PostMapping
    public ListEntity createList(@RequestBody ListCreateRequest req, @AuthenticationPrincipal Jwt jwt) {
        return listService.createList(req.title, req.description, req.adventureIds, jwt.getSubject());
    }

    public record ListCreateRequest(String title, String description, List<Long> adventureIds) {}
}
