package com.sebu.backend.auth.token;

import com.sebu.backend.auth.config.TokenProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;

@Component
public class JwtAccessTokenService {
    private static final String ROLE = "USER";

    private final JwtEncoder jwtEncoder;
    private final TokenProperties properties;
    private final Clock clock;

    @Autowired
    public JwtAccessTokenService(JwtEncoder jwtEncoder, TokenProperties properties) {
        this(jwtEncoder, properties, Clock.systemUTC());
    }

    JwtAccessTokenService(JwtEncoder jwtEncoder, TokenProperties properties, Clock clock) {
        this.jwtEncoder = jwtEncoder;
        this.properties = properties;
        this.clock = clock;
    }

    public String issue(Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("ACCESS_TOKEN_USER_ID_REQUIRED");
        }
        Instant issuedAt = clock.instant();
        JwtClaimsSet claims = JwtClaimsSet.builder()
            .subject(userId.toString())
            .claim("role", ROLE)
            .issuedAt(issuedAt)
            .expiresAt(issuedAt.plus(properties.accessTokenExpiration()))
            .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).type("JWT").build();
        return jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }

    public long expiresInSeconds() {
        return properties.accessTokenExpiration().toSeconds();
    }
}
