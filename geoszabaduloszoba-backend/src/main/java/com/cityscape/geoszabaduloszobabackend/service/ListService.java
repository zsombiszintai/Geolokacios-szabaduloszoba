package com.cityscape.geoszabaduloszobabackend.service;


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

    public List<ListEntity> getMyLists(String sub) {
        return listRepository.findAllByCreatorKeycloakSub(sub);
    }
}
