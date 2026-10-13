package com.cityscape.geoszabaduloszobabackend;

import com.cityscape.geoszabaduloszobabackend.model.entity.AbandonedAdventureEntity;
import com.cityscape.geoszabaduloszobabackend.model.entity.UserEntity;
import com.cityscape.geoszabaduloszobabackend.repository.*;
import com.cityscape.geoszabaduloszobabackend.service.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.server.ResponseStatusException;
import java.util.Optional;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CurrentUserServiceTest {
    @Mock UserRepository users;
    @Mock KeycloakAdminService keycloak;
    @Mock UserService currentUser;
    @Mock AbandonedAdventureRepository abandoned;
    @Mock CompletedAdventureRepository completed;
    @Mock StationRepository stations;
    @Mock AdventureRepository adventures;
    @Mock GameService.GameServiceMapper gameMapper;
    @InjectMocks GameService games;

    @AfterEach void clearContext() { SecurityContextHolder.clearContext(); }

    @Test void userIdentityComesFromTheAuthenticatedSecurityContext() {
        var service = new UserService(users, keycloak);
        var jwt = Jwt.withTokenValue("token").header("alg", "none").subject("subject")
                .claim("preferred_username", "username").build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt, java.util.List.of()));
        assertThat(service.getCurrentUserId()).isEqualTo("subject");
        assertThat(service.getCurrentUsername()).isEqualTo("username");
        verifyNoInteractions(users, keycloak);
    }

    @Test void unauthenticatedIdentityStillFailsWithUnauthorized() {
        var service = new UserService(users, keycloak);
        assertThatThrownBy(service::getCurrentUserId).isInstanceOf(ResponseStatusException.class)
                .satisfies(error -> assertThat(((ResponseStatusException) error).getStatusCode().value()).isEqualTo(401));
    }

    @Test void gameSessionOwnershipUsesTheUserServiceAndRejectsOtherUsers() {
        var owner = new UserEntity();
        owner.setKeycloakSub("owner");
        var session = new AbandonedAdventureEntity();
        session.setUser(owner);
        when(abandoned.findById(1L)).thenReturn(Optional.of(session));
        when(currentUser.getCurrentUserId()).thenReturn("other", "owner");
        assertThatThrownBy(() -> games.getSession(1L)).isInstanceOf(ResponseStatusException.class)
                .satisfies(error -> assertThat(((ResponseStatusException) error).getStatusCode().value()).isEqualTo(403));
        assertThat(games.getSession(1L)).isSameAs(session);
    }
}
