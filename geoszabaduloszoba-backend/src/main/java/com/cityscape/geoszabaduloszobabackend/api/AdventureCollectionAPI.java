package com.cityscape.geoszabaduloszobabackend.api;

import com.cityscape.geoszabaduloszobabackend.model.dto.ListDetailsDTO;
import com.cityscape.geoszabaduloszobabackend.model.entity.ListEntity;
import com.cityscape.geoszabaduloszobabackend.service.ListService;
import com.cityscape.geoszabaduloszobabackend.api.mapper.ApiMapper;
import com.cityscape.geoszabaduloszobabackend.model.dto.AdventureCollectionDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/lists")
@RequiredArgsConstructor
public class AdventureCollectionAPI {

    private final ListService listService;
    private final ApiMapper apiMapper;

    @GetMapping
    public List<AdventureCollectionDTO> getMyLists() {
        return listService.getMyLists().stream().map(apiMapper::toDto).toList();
    }

    @PostMapping
    public ListEntity createList(@RequestBody ListCreateRequest req) {
        return listService.createList(req.title, req.description, req.adventureIds);
    }

    @PostMapping("/{listId}/adventures/{adventureId}")
    public void addAdventureToList(
            @PathVariable Long listId,
            @PathVariable Long adventureId) {

        listService.addAdventureToList(listId, adventureId);
    }

    @DeleteMapping("/{listId}/adventures/{adventureId}")
    public void removeAdventureFromList(
            @PathVariable Long listId,
            @PathVariable Long adventureId) {

        listService.removeAdventureFromList(listId, adventureId);
    }

    @GetMapping("/{listId}")
    public AdventureCollectionDTO getListById(@PathVariable Long listId) {
        return apiMapper.toDto(listService.getListForEditing(listId));
    }

    @GetMapping("/{listId}/details")
    public ListDetailsDTO getListDetails(
            @PathVariable("listId") Long listId
    ) {
        return apiMapper.toListDetailsDto(listService.getListDetails(listId));
    }


    public record ListCreateRequest(String title, String description, List<Long> adventureIds) {}
}
