package com.sebu.backend.global.auth;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class SecurityContextCurrentUserProvider implements CurrentUserProvider {
    @Override
    public Optional<Long> currentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return Optional.empty();
        }
        if (!(authentication.getPrincipal() instanceof Jwt jwt)) {
            return Optional.empty();
        }
        try {
            return Optional.of(Long.parseLong(jwt.getSubject()));
        } catch (NumberFormatException exception) {
            return Optional.empty();
        }
    }
}
