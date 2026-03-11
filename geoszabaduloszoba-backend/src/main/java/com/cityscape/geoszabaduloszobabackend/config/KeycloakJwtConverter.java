package com.cityscape.geoszabaduloszobabackend.config;


import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

@Component
public class KeycloakJwtConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    private final JwtGrantedAuthoritiesConverter defaultConverter = new JwtGrantedAuthoritiesConverter();

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        Collection<GrantedAuthority> authorities = Stream.concat(
                        this.defaultConverter.convert(jwt)
                                .stream(),
                        this.extractRealmRoles(jwt)
                                .stream()
                )
                .toList();

        return new JwtAuthenticationToken(jwt, authorities, jwt.getSubject());
    }

    private Collection<GrantedAuthority> extractRealmRoles(Jwt jwt) {
        Map<String, Object> realmAccess = jwt.getClaim("realm_access");
        if (realmAccess == null) return List.of();

        Object rolesObj = realmAccess.get("roles");
        if (!(rolesObj instanceof List<?> roles)) return List.of();

        return roles.stream()
                .filter(String.class::isInstance)
                .map(r -> new SimpleGrantedAuthority("ROLE_" + r))
                .map(a -> (GrantedAuthority) a)
                .toList();
    }
}
