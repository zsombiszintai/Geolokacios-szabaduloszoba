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
public class ListAPI {

    private final ListService listService;

    @GetMapping
    public List<ListDTO> getMyLists(@AuthenticationPrincipal Jwt jwt) {
        return listService.getMyLists(jwt.getSubject());
    }

    @PostMapping
    public ListEntity createList(@RequestBody ListCreateRequest req, @AuthenticationPrincipal Jwt jwt) {
        return listService.createList(req.title, req.description, req.adventureIds, jwt.getSubject());
    }

    @PostMapping("/{listId}/adventures/{adventureId}")
    public void addAdventureToList(
            @PathVariable Long listId,
            @PathVariable Long adventureId,
            @AuthenticationPrincipal Jwt jwt) {

        listService.addAdventureToList(listId, adventureId, jwt.getSubject());
    }

    @DeleteMapping("/{listId}/adventures/{adventureId}")
    public void removeAdventureFromList(
            @PathVariable Long listId,
            @PathVariable Long adventureId,
            @AuthenticationPrincipal Jwt jwt) {

        listService.removeAdventureFromList(listId, adventureId, jwt.getSubject());
    }

    @GetMapping("/{listId}")
    public ListAPI.ListDTO getListById(@PathVariable Long listId, @AuthenticationPrincipal Jwt jwt) {
        return listService.getListForEditing(listId, jwt.getSubject());
    }

    public record ListDTO(Long id, String title, String description, List<Long> adventureIds) {}

    public record ListCreateRequest(String title, String description, List<Long> adventureIds) {}
}
