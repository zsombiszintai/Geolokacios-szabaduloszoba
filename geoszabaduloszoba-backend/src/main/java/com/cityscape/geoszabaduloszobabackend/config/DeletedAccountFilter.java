package com.cityscape.geoszabaduloszobabackend.config;

import com.cityscape.geoszabaduloszobabackend.repository.AccountDeletionRepository;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;

@RequiredArgsConstructor
public class DeletedAccountFilter extends OncePerRequestFilter {
    private final AccountDeletionRepository deletions;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                  FilterChain chain) throws ServletException, IOException {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof JwtAuthenticationToken jwt
                && deletions.existsById(jwt.getToken().getSubject())) {
            response.sendError(401, "A fiók törlése folyamatban van vagy már befejeződött.");
            return;
        }
        chain.doFilter(request, response);
    }
}
