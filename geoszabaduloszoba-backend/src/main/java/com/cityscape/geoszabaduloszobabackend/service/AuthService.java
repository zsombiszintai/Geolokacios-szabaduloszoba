package com.cityscape.geoszabaduloszobabackend.service;

import com.cityscape.geoszabaduloszobabackend.model.entity.UserEntity;
import com.cityscape.geoszabaduloszobabackend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserEntity login(String username, String rawPassword) {
        UserEntity user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Felhasználó nem található!"));

        if (!passwordEncoder.matches(rawPassword, user.getPassword())) {
            throw new RuntimeException("Hibás jelszó!");
        }

        return user;
    }

    public UserEntity register(UserEntity user) {
        if (userRepository.existsByUsername(user.getUsername())) {
            throw new RuntimeException("A felhasználónév már foglalt!");
        }else{
            user.setPassword(passwordEncoder.encode(user.getPassword()));
            return userRepository.save(user);
        }
    }
}
