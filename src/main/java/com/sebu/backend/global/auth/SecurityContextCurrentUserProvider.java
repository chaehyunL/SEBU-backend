package com.sebu.backend.global.auth;

import com.sebu.backend.user.repository.AppUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class SecurityContextCurrentUserProvider implements CurrentUserProvider {

    private final AppUserRepository appUserRepository;

    @Override
    public Optional<Long> currentUserId() {
        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            return Optional.empty();
        }

        if (!(authentication.getPrincipal() instanceof Jwt jwt)) {
            return Optional.empty();
        }

        Long userId;

        try {
            userId = Long.parseLong(jwt.getSubject());
        } catch (NumberFormatException exception) {
            return Optional.empty();
        }

        return appUserRepository.findById(userId)
                .filter(user -> !user.isDeleted())
                .map(user -> user.getId());
    }
}
