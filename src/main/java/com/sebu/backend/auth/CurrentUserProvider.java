package com.sebu.backend.auth;

import java.util.Optional;

public interface CurrentUserProvider {
    Optional<Long> currentUserId();
}
