package com.cityscape.geoszabaduloszobabackend.service;

import com.cityscape.geoszabaduloszobabackend.model.entity.UserEntity;
import com.cityscape.geoszabaduloszobabackend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final KeycloakAdminService keycloakAdminService;

    public UUID getUserUUID() {
        return UUID.fromString(getJwt().getSubject());
    }

    public String getCurrentUsername() {
        return getJwt().getClaimAsString("preferred_username");
    }

    public String getUsernameByUuid(UUID userUuid) {
        return keycloakAdminService.getUsername(userUuid.toString());
    }

    public boolean isLoggedIn() {
        var authentication = SecurityContextHolder.getContext()
                .getAuthentication();

        return authentication instanceof JwtAuthenticationToken jwtAuth
                && jwtAuth.isAuthenticated();
    }

    public List<String> getRealmRoles() {
        Map<String, Object> realmAccess = getJwt()
                .getClaim("realm_access");

        if (realmAccess == null) {
            return List.of();
        }

        Object roles = realmAccess.get("roles");

        if (!(roles instanceof List<?> rolesList)) {
            return List.of();
        }

        return rolesList.stream()
                .filter(String.class::isInstance)
                .map(String.class::cast)
                .toList();
    }

    @Transactional
    public UserEntity getOrCreateCurrentUser() {
        String keycloakSub = getJwt().getSubject();

        return userRepository.findByKeycloakSub(keycloakSub)
                .orElseGet(() -> {
                    UserEntity user = new UserEntity();
                    user.setKeycloakSub(keycloakSub);

                    return userRepository.save(user);
                });
    }

    @Transactional
    public void updateDescription(String description) {
        UserEntity user = getOrCreateCurrentUser();
        user.setProfileDescription(description);
        userRepository.save(user);
    }

    @Transactional
    public void updateAvatarKey(String keycloakSub, String objectKey) {
        if (!getJwt().getSubject().equals(keycloakSub)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Csak a saját profilképedet módosíthatod."
            );
        }

        UserEntity user = getOrCreateCurrentUser();
        user.setProfilePictureUrl(objectKey);
        userRepository.save(user);
    }

    private Jwt getJwt() {
        var authentication = SecurityContextHolder.getContext()
                .getAuthentication();

        if (authentication instanceof JwtAuthenticationToken jwtAuth
                && jwtAuth.isAuthenticated()) {
            return jwtAuth.getToken();
        }

        throw new ResponseStatusException(
                HttpStatus.UNAUTHORIZED,
                "Bejelentkezés szükséges."
        );
    }
}