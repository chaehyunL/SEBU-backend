package com.sebu.backend.auth.config;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.net.URI;
import java.time.Duration;

@Validated
@ConfigurationProperties(prefix = "app.auth.sejong")
public record SejongClientProperties(
    @NotNull URI portalLoginUrl,
    @NotNull URI ssoLoginUrl,
    @NotNull URI userInfoUrl,
    @NotNull Duration connectTimeout,
    @NotNull Duration requestTimeout
) {
    private static final String PORTAL_HOST = "portal.sejong.ac.kr";
    private static final String SJPT_HOST = "sjpt.sejong.ac.kr";

    @AssertTrue(message = "sejong client timeouts must be positive")
    public boolean isTimeoutConfigurationValid() {
        return isPositive(connectTimeout) && isPositive(requestTimeout);
    }

    @AssertTrue(message = "sejong client endpoints must use the official HTTPS hosts")
    public boolean isEndpointConfigurationValid() {
        return isOfficialHttpsEndpoint(portalLoginUrl, PORTAL_HOST)
            && isOfficialHttpsEndpoint(ssoLoginUrl, SJPT_HOST)
            && isOfficialHttpsEndpoint(userInfoUrl, SJPT_HOST);
    }

    private static boolean isPositive(Duration duration) {
        return duration != null && !duration.isZero() && !duration.isNegative();
    }

    private static boolean isOfficialHttpsEndpoint(URI uri, String expectedHost) {
        return uri != null
            && "https".equalsIgnoreCase(uri.getScheme())
            && expectedHost.equalsIgnoreCase(uri.getHost())
            && (uri.getPort() == -1 || uri.getPort() == 443)
            && uri.getRawUserInfo() == null
            && uri.getRawQuery() == null
            && uri.getRawFragment() == null;
    }
}
