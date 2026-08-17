package com.sebu.backend.global.auth;

import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class AnonymousCurrentUserProvider implements CurrentUserProvider {
    @Override
    public Optional<Long> currentUserId() {
        return Optional.empty();
    }
}
