package com.sebu.backend.auth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sebu.backend.auth.config.TokenProperties;
import com.sebu.backend.auth.port.SejongAuthenticationException;
import com.sebu.backend.auth.port.SejongAuthenticator;
import com.sebu.backend.auth.port.SejongIdentity;
import com.sebu.backend.auth.repository.RefreshTokenRepository;
import com.sebu.backend.auth.token.RefreshTokenGenerator;
import com.sebu.backend.user.domain.AuthProvider;
import com.sebu.backend.user.repository.AppUserRepository;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "app.rate-limit.login.max-requests=100")
@AutoConfigureMockMvc
@Transactional
class AuthApiIntegrationTest {
    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    AppUserRepository appUserRepository;

    @Autowired
    RefreshTokenRepository refreshTokenRepository;

    @Autowired
    RefreshTokenGenerator refreshTokenGenerator;

    @Autowired
    JwtEncoder jwtEncoder;

    @Autowired
    TokenProperties tokenProperties;

    @MockitoBean
    SejongAuthenticator sejongAuthenticator;

    @BeforeEach
    void setUpAuthenticator() {
        when(sejongAuthenticator.authenticate(anyString(), anyString()))
            .thenAnswer(invocation -> new SejongIdentity(
                invocation.getArgument(0),
                "RUNNING",
                "20260818120000",
                "STUDENT"
            ));
    }

    @Test
    void logsInNewSejongUserAndSetsSecureRefreshCookie() throws Exception {
        mockMvc.perform(loginRequest("21012345", "password"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
            .andExpect(jsonPath("$.data.tokenType").value("Bearer"))
            .andExpect(jsonPath("$.data.expiresIn").value(1800))
            .andExpect(jsonPath("$.data.user.isNewUser").value(true))
            .andExpect(jsonPath("$.data.user.profileCompleted").value(false))
            .andExpect(header().string(HttpHeaders.SET_COOKIE, allOf(
                containsString("refresh_token="),
                containsString("Path=/api/v1/auth"),
                containsString("Max-Age=1209600"),
                containsString("Secure"),
                containsString("HttpOnly"),
                containsString("SameSite=Lax")
            )));

        assertThat(appUserRepository.findByProviderAndProviderUserId(AuthProvider.SEJONG, "21012345"))
            .isPresent();
        assertThat(refreshTokenRepository.count()).isOne();
    }

    @Test
    void reusesExistingSejongUserWithoutRevokingOtherLogin() throws Exception {
        MvcResult first = mockMvc.perform(loginRequest("21012345", "password"))
            .andExpect(status().isOk())
            .andReturn();

        mockMvc.perform(loginRequest("21012345", "password"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.user.isNewUser").value(false));

        assertThat(appUserRepository.count()).isOne();
        Long userId = appUserRepository.findByProviderAndProviderUserId(AuthProvider.SEJONG, "21012345")
            .orElseThrow()
            .getId();
        assertThat(refreshTokenRepository.countByUser_Id(userId)).isEqualTo(2);
        assertThat(refreshTokenRepository.findByTokenHash(
            refreshTokenGenerator.hash(refreshTokenFrom(first))
        )).get().extracting(token -> token.getRevokedAt()).isNull();
    }

    @Test
    void mapsInvalidRequestAndSejongFailuresToApiContract() throws Exception {
        mockMvc.perform(post("/api/v1/auth/sejong/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error.code").value("INVALID_LOGIN_REQUEST"));

        when(sejongAuthenticator.authenticate("bad-user", "bad-password"))
            .thenThrow(SejongAuthenticationException.authenticationFailed());
        mockMvc.perform(loginRequest("bad-user", "bad-password"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.error.code").value("SEJONG_AUTH_FAILED"));

        when(sejongAuthenticator.authenticate("system-error", "password"))
            .thenThrow(SejongAuthenticationException.systemUnavailable());
        mockMvc.perform(loginRequest("system-error", "password"))
            .andExpect(status().isBadGateway())
            .andExpect(jsonPath("$.error.code").value("SEJONG_SYSTEM_UNAVAILABLE"));
    }

    @Test
    void rotatesRefreshTokenAndRejectsPreviousTokenWithoutCallingSejong() throws Exception {
        MvcResult login = mockMvc.perform(loginRequest("rotation-user", "password"))
            .andExpect(status().isOk())
            .andReturn();
        String previousToken = refreshTokenFrom(login);
        clearInvocations(sejongAuthenticator);

        MvcResult refresh = mockMvc.perform(post("/api/v1/auth/refresh")
                .cookie(new Cookie(RefreshTokenCookieFactory.COOKIE_NAME, previousToken)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
            .andExpect(jsonPath("$.data.tokenType").value("Bearer"))
            .andExpect(jsonPath("$.data.expiresIn").value(1800))
            .andReturn();

        String rotatedToken = refreshTokenFrom(refresh);
        assertThat(rotatedToken).isNotEqualTo(previousToken);
        verifyNoInteractions(sejongAuthenticator);

        mockMvc.perform(post("/api/v1/auth/refresh")
                .cookie(new Cookie(RefreshTokenCookieFactory.COOKIE_NAME, previousToken)))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.error.code").value("REFRESH_TOKEN_INVALID"));
    }

    @Test
    void refreshesWithValidCookieEvenWhenExpiredAccessTokenHeaderIsPresent() throws Exception {
        MvcResult login = mockMvc.perform(loginRequest("expired-header-user", "password"))
            .andExpect(status().isOk())
            .andReturn();

        mockMvc.perform(post("/api/v1/auth/refresh")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + expiredAccessToken())
                .cookie(new Cookie(RefreshTokenCookieFactory.COOKIE_NAME, refreshTokenFrom(login))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.accessToken").isNotEmpty());
    }

    @Test
    void refreshesWithValidCookieEvenWhenInvalidAccessTokenHeaderIsPresent() throws Exception {
        MvcResult login = mockMvc.perform(loginRequest("invalid-header-user", "password"))
            .andExpect(status().isOk())
            .andReturn();

        mockMvc.perform(post("/api/v1/auth/refresh")
                .header(HttpHeaders.AUTHORIZATION, "Bearer invalid-access-token")
                .cookie(new Cookie(RefreshTokenCookieFactory.COOKIE_NAME, refreshTokenFrom(login))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.accessToken").isNotEmpty());
    }

    @Test
    void rejectsRefreshWithoutCookie() throws Exception {
        mockMvc.perform(post("/api/v1/auth/refresh"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.error.code").value("REFRESH_TOKEN_INVALID"));
    }

    @Test
    void logsOutOnlyCurrentTokenAndClearsCookie() throws Exception {
        MvcResult firstLogin = mockMvc.perform(loginRequest("multi-device-user", "password"))
            .andExpect(status().isOk())
            .andReturn();
        MvcResult secondLogin = mockMvc.perform(loginRequest("multi-device-user", "password"))
            .andExpect(status().isOk())
            .andReturn();
        String firstToken = refreshTokenFrom(firstLogin);
        String secondToken = refreshTokenFrom(secondLogin);

        mockMvc.perform(post("/api/v1/auth/logout")
                .cookie(new Cookie(RefreshTokenCookieFactory.COOKIE_NAME, firstToken)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.message").value("로그아웃되었습니다."))
            .andExpect(header().string(HttpHeaders.SET_COOKIE, allOf(
                containsString("refresh_token="),
                containsString("Max-Age=0"),
                containsString("Path=/api/v1/auth"),
                containsString("Secure"),
                containsString("HttpOnly"),
                containsString("SameSite=Lax")
            )));

        mockMvc.perform(post("/api/v1/auth/refresh")
                .cookie(new Cookie(RefreshTokenCookieFactory.COOKIE_NAME, firstToken)))
            .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/v1/auth/logout")
                .cookie(new Cookie(RefreshTokenCookieFactory.COOKIE_NAME, firstToken)))
            .andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/auth/refresh")
                .cookie(new Cookie(RefreshTokenCookieFactory.COOKIE_NAME, secondToken)))
            .andExpect(status().isOk());
    }

    @Test
    void logoutWithoutCookieIsIdempotent() throws Exception {
        mockMvc.perform(post("/api/v1/auth/logout"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.message").value("로그아웃되었습니다."))
            .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("Max-Age=0")));
    }

    private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder loginRequest(
        String studentId,
        String password
    ) throws Exception {
        return post("/api/v1/auth/sejong/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(new LoginRequestFixture(studentId, password)));
    }

    private String refreshTokenFrom(MvcResult result) {
        String setCookie = result.getResponse().getHeader(HttpHeaders.SET_COOKIE);
        String prefix = RefreshTokenCookieFactory.COOKIE_NAME + "=";
        int start = setCookie.indexOf(prefix) + prefix.length();
        int end = setCookie.indexOf(';', start);
        return setCookie.substring(start, end);
    }

    private String expiredAccessToken() {
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
            .subject("1")
            .claim("role", "USER")
            .issuedAt(now.minus(tokenProperties.accessTokenExpiration()).minusSeconds(120))
            .expiresAt(now.minusSeconds(120))
            .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).type("JWT").build();
        return jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }

    private record LoginRequestFixture(String studentId, String password) {
    }
}
