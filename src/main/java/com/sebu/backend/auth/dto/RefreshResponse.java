package com.sebu.backend.auth.dto;

import com.sebu.backend.auth.service.AuthSessionService;

public record RefreshResponse(String accessToken, String tokenType, long expiresIn) {
    public static RefreshResponse from(AuthSessionService.RefreshSession session) {
        return new RefreshResponse(session.accessToken(), "Bearer", session.expiresIn());
    }

    @Override
    public String toString() {
        return "RefreshResponse[accessToken=REDACTED, tokenType=" + tokenType
            + ", expiresIn=" + expiresIn + "]";
    }
}
