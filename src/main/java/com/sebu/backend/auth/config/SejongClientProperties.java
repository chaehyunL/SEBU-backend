package com.sebu.backend.auth.config;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.net.URI;
import java.time.Duration;

@Validated
@ConfigurationProperties(prefix = "app.auth.sejong")
public record SejongClientProperties(
    @NotNull URI portalLoginUrl,
    @NotNull URI portalLoginPageUrl,
    @NotBlank String portalReturnUrl,
    @NotNull URI portalSsoLoginUrl,
    @NotNull URI ssoLoginUrl,
    @NotNull URI userInfoUrl,
    @NotNull Duration connectTimeout,
    @NotNull Duration requestTimeout
) {
    @AssertTrue(message = "sejong client timeouts must be positive")
    public boolean areTimeoutsPositive() {
        return isPositive(connectTimeout) && isPositive(requestTimeout);
    }

    private static boolean isPositive(Duration duration) {
        return duration != null && !duration.isZero() && !duration.isNegative();
    }
}
