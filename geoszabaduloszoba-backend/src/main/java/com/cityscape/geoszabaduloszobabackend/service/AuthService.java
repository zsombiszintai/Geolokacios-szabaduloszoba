package com.cityscape.geoszabaduloszobabackend.service;

import com.cityscape.geoszabaduloszobabackend.model.entity.UserEntity;
import com.cityscape.geoszabaduloszobabackend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository userRepository;

    public UserEntity login(String username, String password){
        return userRepository.findByUsername(username)
                .filter(user -> user.getPassword().equals(password))
                .orElseThrow(() -> new RuntimeException("Érvénytelen adatok."));
    }
}
