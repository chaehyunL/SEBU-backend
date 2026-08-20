package com.sebu.backend.global.auth;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SecurityContextCurrentUserProviderTest {
    private final SecurityContextCurrentUserProvider provider = new SecurityContextCurrentUserProvider();

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void returnsAuthenticatedUserIdFromJwtSubject() {
        Jwt jwt = Jwt.withTokenValue("token")
            .header("alg", "HS256")
            .subject("17")
            .claim("role", "USER")
            .issuedAt(Instant.now())
            .expiresAt(Instant.now().plusSeconds(1800))
            .build();
        SecurityContextHolder.getContext().setAuthentication(
            new JwtAuthenticationToken(jwt, List.of(new SimpleGrantedAuthority("ROLE_USER")))
        );

        assertThat(provider.currentUserId()).contains(17L);
    }

    @Test
    void returnsEmptyForAnonymousRequest() {
        assertThat(provider.currentUserId()).isEmpty();
    }
}
