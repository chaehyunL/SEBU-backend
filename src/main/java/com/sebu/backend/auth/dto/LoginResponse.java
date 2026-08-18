package com.sebu.backend.auth.dto;

import com.sebu.backend.auth.service.AuthSessionService;

public record LoginResponse(
    String accessToken,
    String tokenType,
    long expiresIn,
    UserResponse user
) {
    public static LoginResponse from(AuthSessionService.LoginSession session) {
        return new LoginResponse(
            session.accessToken(),
            "Bearer",
            session.expiresIn(),
            new UserResponse(session.userId(), session.isNewUser(), session.isProfileCompleted())
        );
    }

    @Override
    public String toString() {
        return "LoginResponse[accessToken=REDACTED, tokenType=" + tokenType
            + ", expiresIn=" + expiresIn + ", user=" + user + "]";
    }

    public record UserResponse(Long id, boolean isNewUser, boolean profileCompleted) {
    }
}
