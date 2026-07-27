package com.cityscape.geoszabaduloszobabackend.service;


import com.cityscape.geoszabaduloszobabackend.api.ListAPI;
import com.cityscape.geoszabaduloszobabackend.model.entity.AdventureEntity;
import com.cityscape.geoszabaduloszobabackend.model.entity.ListEntity;
import com.cityscape.geoszabaduloszobabackend.model.entity.UserEntity;
import com.cityscape.geoszabaduloszobabackend.repository.AdventureRepository;
import com.cityscape.geoszabaduloszobabackend.repository.ListRepository;
import com.cityscape.geoszabaduloszobabackend.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ListService {
    private final ListRepository listRepository;
    private final UserRepository userRepository;
    private final AdventureRepository adventureRepository;

    @Transactional
    public ListEntity createList(String title, String description, List<Long> adventureIds, String keycloakSub) {
        UserEntity creator = userRepository.findByKeycloakSub(keycloakSub)
                .orElseThrow(() -> new RuntimeException("Felhasználó nem található"));

        ListEntity list = new ListEntity();
        list.setTitle(title);
        list.setDescription(description);
        list.setCreator(creator);

        List<AdventureEntity> adventures = adventureRepository.findAllById(adventureIds);
        list.setAdventures(adventures);

        return listRepository.save(list);
    }

    @Transactional
    public void addAdventureToList(Long listId, Long adventureId, String keycloakSub) {
        ListEntity list = listRepository.findById(listId)
                .orElseThrow(() -> new RuntimeException("A lista nem található"));

        if (!list.getCreator().getKeycloakSub().equals(keycloakSub)) {
            throw new RuntimeException("Nincs jogosultságod módosítani ezt a listát");
        }

        AdventureEntity adventure = adventureRepository.findById(adventureId)
                .orElseThrow(() -> new RuntimeException("A kaland nem található"));

        if (!list.getAdventures().contains(adventure)) {
            list.getAdventures().add(adventure);
            listRepository.save(list);
        }
    }

    @Transactional
    public List<ListAPI.ListDTO> getMyLists(String sub) {
        return listRepository.findAllByCreatorKeycloakSub(sub).stream()
                .map(list -> new ListAPI.ListDTO(
                        list.getId(),
                        list.getTitle(),
                        list.getDescription(),
                        list.getAdventures().stream()
                                .map(AdventureEntity::getId)
                                .collect(Collectors.toList())
                ))
                .collect(Collectors.toList());
    }

    @Transactional
    public void removeAdventureFromList(Long listId, Long adventureId, String keycloakSub) {
        ListEntity list = listRepository.findById(listId)
                .orElseThrow(() -> new RuntimeException("A lista nem található"));

        if (!list.getCreator().getKeycloakSub().equals(keycloakSub)) {
            throw new RuntimeException("Nincs jogosultságod módosítani ezt a listát");
        }

        AdventureEntity adventure = adventureRepository.findById(adventureId)
                .orElseThrow(() -> new RuntimeException("A kaland nem található"));

        if (list.getAdventures().contains(adventure)) {
            list.getAdventures().remove(adventure);
            listRepository.save(list);
        }
    }

    @Transactional
    public ListAPI.ListDTO getListForEditing(Long listId, String keycloakSub) {
        ListEntity list = listRepository.findById(listId)
                .orElseThrow(() -> new RuntimeException("A lista nem található"));

        if (!list.getCreator().getKeycloakSub().equals(keycloakSub)) {
            throw new RuntimeException("Nincs jogosultságod a lista szerkesztéséhez!");
        }

        return new ListAPI.ListDTO(
                list.getId(),
                list.getTitle(),
                list.getDescription(),
                list.getAdventures().stream()
                        .map(AdventureEntity::getId)
                        .collect(Collectors.toList())
        );
    }
}
