package com.sebu.backend.auth.token;

import com.sebu.backend.auth.config.TokenProperties;
import com.sebu.backend.user.domain.AppUser;
import com.sebu.backend.user.repository.AppUserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class JwtSecurityIntegrationTest {
    @Autowired
    MockMvc mockMvc;

    @Autowired
    JwtAccessTokenService accessTokenService;

    @Autowired
    JwtEncoder jwtEncoder;

    @Autowired
    TokenProperties properties;

    @Autowired
    AppUserRepository appUserRepository;

    @Test
    void authenticatesBearerTokenAndExposesCurrentUserId() throws Exception {
        AppUser user = appUserRepository.save(AppUser.sejong("jwt-user"));
        mockMvc.perform(get("/api/v1/me")
                .header("Authorization", "Bearer " + accessTokenService.issue(user.getId())))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.id").value(user.getId()))
            .andExpect(jsonPath("$.data.nickname").doesNotExist())
            .andExpect(jsonPath("$.data.profileCompleted").value(false));
    }

    @Test
    void returnsCommonResponseForMissingAccessToken() throws Exception {
        mockMvc.perform(get("/api/v1/me"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.error.code").value("ACCESS_TOKEN_INVALID"));
    }

    @Test
    void keepsExistingLaboratoryApiPublic() throws Exception {
        mockMvc.perform(get("/api/v1/laboratories"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void returnsExpiredErrorForExpiredAccessToken() throws Exception {
        AppUser user = appUserRepository.save(AppUser.sejong("expired-jwt-user"));
        Instant issuedAt = Instant.now().minus(properties.accessTokenExpiration()).minusSeconds(1);
        JwtAccessTokenService expiredIssuer = new JwtAccessTokenService(
            jwtEncoder,
            properties,
            Clock.fixed(issuedAt, ZoneOffset.UTC)
        );

        mockMvc.perform(get("/api/v1/me")
                .header("Authorization", "Bearer " + expiredIssuer.issue(user.getId())))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.error.code").value("ACCESS_TOKEN_EXPIRED"));
    }

    @Test
    void returnsInvalidErrorForMalformedAccessToken() throws Exception {
        mockMvc.perform(get("/api/v1/me")
                .header("Authorization", "Bearer malformed-token"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.error.code").value("ACCESS_TOKEN_INVALID"));
    }
}
