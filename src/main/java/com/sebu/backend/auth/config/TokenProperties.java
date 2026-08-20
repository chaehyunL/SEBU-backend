package com.sebu.backend.auth.config;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;
import java.util.Base64;

@Validated
@ConfigurationProperties(prefix = "app.auth.token")
public record TokenProperties(
    @NotBlank String jwtSecretBase64,
    @NotNull Duration accessTokenExpiration,
    @NotNull Duration refreshTokenExpiration
) {
    private static final int MINIMUM_HS256_KEY_BYTES = 32;

    @AssertTrue(message = "token expirations must be positive")
    public boolean areExpirationsPositive() {
        return isPositive(accessTokenExpiration) && isPositive(refreshTokenExpiration);
    }

    @AssertTrue(message = "jwt secret must be valid Base64 containing at least 256 bits")
    public boolean isJwtSecretValid() {
        if (jwtSecretBase64 == null || jwtSecretBase64.isBlank()) {
            return false;
        }
        try {
            return Base64.getDecoder().decode(jwtSecretBase64).length >= MINIMUM_HS256_KEY_BYTES;
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }

    public byte[] jwtSecretBytes() {
        return Base64.getDecoder().decode(jwtSecretBase64);
    }

    private static boolean isPositive(Duration duration) {
        return duration != null && !duration.isZero() && !duration.isNegative();
    }
}
