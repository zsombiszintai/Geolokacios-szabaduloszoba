package com.cityscape.geoszabaduloszobabackend.service;

import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.WebApplicationException;
import lombok.RequiredArgsConstructor;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class KeycloakAdminService {

    private final Keycloak keycloak;

    @Value("${keycloak.admin.realm}")
    private String realm;

    public List<UserRepresentation> searchByUsername(String term) {
        if (term == null || term.isBlank()) {
            return List.of();
        }

        try {
            return keycloak.realm(realm)
                    .users()
                    .search(
                            term.trim(), // username
                            null,        // firstName
                            null,        // lastName
                            null,        // email
                            0,           // első találat
                            50,          // maximum találatszám
                            true,        // csak engedélyezett fiókok
                            true,        // rövid reprezentáció
                            false        // részleges névegyezés
                    );
        } catch (WebApplicationException | ProcessingException e) {
            throw unavailable(e);
        }
    }

    public UserRepresentation getById(String keycloakSub) {
        try {
            return keycloak.realm(realm)
                    .users()
                    .get(keycloakSub)
                    .toRepresentation();
        } catch (NotFoundException e) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "A felhasználó nem található.",
                    e
            );
        } catch (WebApplicationException | ProcessingException e) {
            throw unavailable(e);
        }
    }

    public UserRepresentation getByUsername(String username) {
        if (username == null || username.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "A felhasználónév megadása kötelező."
            );
        }

        try {
            return keycloak.realm(realm)
                    .users()
                    .searchByUsername(username.trim(), true)
                    .stream()
                    .findFirst()
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.NOT_FOUND,
                            "A felhasználó nem található."
                    ));
        } catch (WebApplicationException | ProcessingException e) {
            throw unavailable(e);
        }
    }

    public String getUsername(String keycloakSub) {
        return getById(keycloakSub).getUsername();
    }

    private ResponseStatusException unavailable(Exception cause) {
        return new ResponseStatusException(
                HttpStatus.SERVICE_UNAVAILABLE,
                "A felhasználói adatok jelenleg nem kérhetők le.",
                cause
        );
    }
}