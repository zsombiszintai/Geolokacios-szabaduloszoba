package com.cityscape.geoszabaduloszobabackend;

import com.cityscape.geoszabaduloszobabackend.config.DeletedAccountFilter;
import com.cityscape.geoszabaduloszobabackend.repository.AccountDeletionRepository;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class DeletedAccountFilterTest {
    @AfterEach
    void cleanup() { SecurityContextHolder.clearContext(); }

    @Test
    void rejectsOldTokensForDeletionRequestsAndAllowsOtherAccounts() throws Exception {
        var repository = mock(AccountDeletionRepository.class);
        var chain = mock(FilterChain.class);
        var request = new MockHttpServletRequest("POST", "/settings/description");
        var response = new MockHttpServletResponse();
        var jwt = Jwt.withTokenValue("old-token").header("alg", "RS256").subject("owner").build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));
        when(repository.existsById("owner")).thenReturn(true);
        new DeletedAccountFilter(repository).doFilter(request, response, chain);
        assertThat(response.getStatus()).isEqualTo(401);
        verifyNoInteractions(chain);

        when(repository.existsById("owner")).thenReturn(false);
        var allowedResponse = new MockHttpServletResponse();
        new DeletedAccountFilter(repository).doFilter(request, allowedResponse, chain);
        verify(chain).doFilter(request, allowedResponse);
    }
}
