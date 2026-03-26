package com.cityscape.geoszabaduloszobabackend.service;

import com.cityscape.geoszabaduloszobabackend.model.entity.UserEntity;
import com.cityscape.geoszabaduloszobabackend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    public UUID getUserUUID() {
        return UUID.fromString(this.getJwt()
                .getSubject());
    }

    private String getUserPreferredUsername() {
        return this.getJwt()
                .getClaimAsString("preferred_username");
    }

    public Boolean isLoggedIn() {
        return !(SecurityContextHolder.getContext()
                .getAuthentication() instanceof AnonymousAuthenticationToken);
    }

    private Jwt getJwt() {
        var auth = (JwtAuthenticationToken) SecurityContextHolder.getContext()
                .getAuthentication();
        return auth.getToken();
    }

    public UserEntity getOrCreateCurrentUser() {
        String keycloakSub = this.getJwt().getSubject();
        String username = this.getJwt().getClaimAsString("preferred_username");
        String email = this.getJwt().getClaimAsString("email");

        return userRepository.findByKeycloakSub(keycloakSub)
                .or(() -> userRepository.findByEmail(email))
                .map(user -> {
                    if (user.getKeycloakSub() == null) {
                        user.setKeycloakSub(keycloakSub);
                        return userRepository.save(user);
                    }
                    return user;
                })
                .orElseGet(() -> {
                    UserEntity newUser = new UserEntity();
                    newUser.setKeycloakSub(keycloakSub);
                    newUser.setUsername(username);
                    newUser.setEmail(email);
                    return userRepository.save(newUser);
                });
    }
}
