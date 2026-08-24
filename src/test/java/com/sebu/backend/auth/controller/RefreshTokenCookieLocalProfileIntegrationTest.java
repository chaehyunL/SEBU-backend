package com.sebu.backend.auth.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:cookie-local;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
        "spring.flyway.locations=classpath:db/migration",
        "app.auth.token.jwt-secret-base64=MDEyMzQ1Njc4OTAxMjM0NTY3ODkwMTIzNDU2Nzg5MDE="
})
@ActiveProfiles("local")
class RefreshTokenCookieLocalProfileIntegrationTest {

    @Autowired
    RefreshTokenCookieFactory cookieFactory;

    @Test
    void createsNonSecureRefreshCookieForLocalHttpProfile() {
        assertThat(cookieFactory.create("local-refresh-token").isSecure()).isFalse();
        assertThat(cookieFactory.delete().isSecure()).isFalse();
    }
}
