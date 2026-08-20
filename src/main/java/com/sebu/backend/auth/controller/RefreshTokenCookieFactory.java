package com.sebu.backend.auth.controller;

import com.sebu.backend.auth.config.AuthCookieProperties;
import com.sebu.backend.auth.config.TokenProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@RequiredArgsConstructor
public class RefreshTokenCookieFactory {
    public static final String COOKIE_NAME = "refresh_token";
    private static final String COOKIE_PATH = "/api/v1/auth";

    private final TokenProperties properties;
    private final AuthCookieProperties cookieProperties;

    public ResponseCookie create(String refreshToken) {
        return base(refreshToken)
            .maxAge(properties.refreshTokenExpiration())
            .build();
    }

    public ResponseCookie delete() {
        return base("")
            .maxAge(Duration.ZERO)
            .build();
    }

    private ResponseCookie.ResponseCookieBuilder base(String value) {
        return ResponseCookie.from(COOKIE_NAME, value)
            .httpOnly(true)
            .secure(cookieProperties.secure())
            .sameSite("Lax")
            .path(COOKIE_PATH);
    }
}
