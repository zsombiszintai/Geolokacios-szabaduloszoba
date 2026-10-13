package com.cityscape.geoszabaduloszobabackend.service;


import com.cityscape.geoszabaduloszobabackend.model.entity.AdventureEntity;
import com.cityscape.geoszabaduloszobabackend.model.entity.ListEntity;
import com.cityscape.geoszabaduloszobabackend.model.entity.UserEntity;
import com.cityscape.geoszabaduloszobabackend.repository.AdventureRepository;
import com.cityscape.geoszabaduloszobabackend.repository.ListRepository;
import com.cityscape.geoszabaduloszobabackend.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ListService {
    private final UserService userService;
    private final ListRepository listRepository;
    private final UserRepository userRepository;
    private final AdventureRepository adventureRepository;

    @Transactional
    public ListEntity createList(String title, String description, List<Long> adventureIds) {
        String keycloakSub = userService.getCurrentUserId();
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
    public void addAdventureToList(Long listId, Long adventureId) {
        String keycloakSub = userService.getCurrentUserId();
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
    public List<ListEntity> getMyLists() {
        return listRepository.findAllByCreatorKeycloakSub(userService.getCurrentUserId());
    }

    @Transactional
    public void removeAdventureFromList(Long listId, Long adventureId) {
        String keycloakSub = userService.getCurrentUserId();
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
    public ListEntity getListForEditing(Long listId) {
        String sub = userService.getCurrentUserId();
        ListEntity list = listRepository.findById(listId)
                .orElseThrow(() -> new RuntimeException("A lista nem található"));
        if (!list.getCreator().getKeycloakSub().equals(sub)) {
            throw new RuntimeException("Nincs jogosultságod a lista szerkesztéséhez!");
        }
        return list;
    }

    @Transactional
    public ListEntity getListDetails(Long listId) {
        return listRepository.findById(listId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "A lista nem található."));
    }
}
