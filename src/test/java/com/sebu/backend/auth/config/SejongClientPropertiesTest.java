package com.sebu.backend.auth.config;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class SejongClientPropertiesTest {
    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void acceptsOfficialHttpsEndpoints() {
        SejongClientProperties properties = new SejongClientProperties(
            URI.create("https://portal.sejong.ac.kr/jsp/login/login_action.jsp"),
            URI.create("https://sjpt.sejong.ac.kr/main/view/Login/doSsoLogin.do"),
            URI.create("https://sjpt.sejong.ac.kr/main/sys/UserInfo/initUserInfo.do"),
            Duration.ofSeconds(5),
            Duration.ofSeconds(10)
        );

        assertThat(validator.validate(properties)).isEmpty();
    }

    @Test
    void rejectsPlainHttpAndUnapprovedHosts() {
        SejongClientProperties properties = new SejongClientProperties(
            URI.create("http://portal.sejong.ac.kr/jsp/login/login_action.jsp"),
            URI.create("https://attacker.example/main/view/Login/doSsoLogin.do"),
            URI.create("https://sjpt.sejong.ac.kr/main/sys/UserInfo/initUserInfo.do"),
            Duration.ofSeconds(5),
            Duration.ofSeconds(10)
        );

        assertThat(validator.validate(properties))
            .extracting(violation -> violation.getMessage())
            .contains("sejong client endpoints must use the official HTTPS hosts");
    }

    @Test
    void rejectsZeroOrNegativeTimeouts() {
        SejongClientProperties properties = new SejongClientProperties(
            URI.create("https://portal.sejong.ac.kr/jsp/login/login_action.jsp"),
            URI.create("https://sjpt.sejong.ac.kr/main/view/Login/doSsoLogin.do"),
            URI.create("https://sjpt.sejong.ac.kr/main/sys/UserInfo/initUserInfo.do"),
            Duration.ZERO,
            Duration.ofSeconds(-1)
        );

        assertThat(validator.validate(properties))
            .extracting(violation -> violation.getMessage())
            .contains("sejong client timeouts must be positive");
    }
}
