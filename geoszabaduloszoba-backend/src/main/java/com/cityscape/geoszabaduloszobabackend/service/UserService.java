package com.cityscape.geoszabaduloszobabackend.service;

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
}
