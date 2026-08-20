package com.sebu.backend.auth.service;

import com.sebu.backend.auth.config.TokenProperties;
import com.sebu.backend.auth.domain.RefreshToken;
import com.sebu.backend.auth.exception.RefreshTokenInvalidException;
import com.sebu.backend.auth.repository.RefreshTokenRepository;
import com.sebu.backend.auth.token.JwtAccessTokenService;
import com.sebu.backend.auth.token.RefreshTokenGenerator;
import com.sebu.backend.user.domain.AppUser;
import com.sebu.backend.user.domain.AuthProvider;
import com.sebu.backend.user.repository.AppUserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

@Service
public class AuthSessionService {
    private final AppUserRepository appUserRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final RefreshTokenGenerator refreshTokenGenerator;
    private final JwtAccessTokenService accessTokenService;
    private final TokenProperties properties;
    private final Clock clock;

    @Autowired
    public AuthSessionService(
        AppUserRepository appUserRepository,
        RefreshTokenRepository refreshTokenRepository,
        RefreshTokenGenerator refreshTokenGenerator,
        JwtAccessTokenService accessTokenService,
        TokenProperties properties
    ) {
        this(
            appUserRepository,
            refreshTokenRepository,
            refreshTokenGenerator,
            accessTokenService,
            properties,
            Clock.systemUTC()
        );
    }

    AuthSessionService(
        AppUserRepository appUserRepository,
        RefreshTokenRepository refreshTokenRepository,
        RefreshTokenGenerator refreshTokenGenerator,
        JwtAccessTokenService accessTokenService,
        TokenProperties properties,
        Clock clock
    ) {
        this.appUserRepository = appUserRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.refreshTokenGenerator = refreshTokenGenerator;
        this.accessTokenService = accessTokenService;
        this.properties = properties;
        this.clock = clock;
    }

    @Transactional
    public LoginSession start(String providerUserId) {
        AppUser user = appUserRepository
            .findByProviderAndProviderUserId(AuthProvider.SEJONG, providerUserId)
            .orElse(null);
        boolean newUser = user == null;
        if (newUser) {
            user = appUserRepository.save(AppUser.sejong(providerUserId));
        }

        return issueLoginSession(user, newUser, now());
    }

    @Transactional
    public Optional<LoginSession> startExisting(String providerUserId) {
        return appUserRepository.findByProviderAndProviderUserId(AuthProvider.SEJONG, providerUserId)
            .map(user -> issueLoginSession(user, false, now()));
    }

    @Transactional
    public RefreshSession refresh(String rawRefreshToken) {
        if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
            throw new RefreshTokenInvalidException();
        }
        String tokenHash = refreshTokenGenerator.hash(rawRefreshToken);
        RefreshToken currentToken = refreshTokenRepository.findByTokenHashForUpdate(tokenHash)
            .orElseThrow(RefreshTokenInvalidException::new);
        LocalDateTime now = now();
        if (!currentToken.isUsableAt(now)) {
            throw new RefreshTokenInvalidException();
        }

        currentToken.revoke(now);
        IssuedRefreshToken newRefreshToken = issueRefreshToken(currentToken.getUser(), now);
        return new RefreshSession(
            accessTokenService.issue(currentToken.getUser().getId()),
            accessTokenService.expiresInSeconds(),
            newRefreshToken.rawToken()
        );
    }

    @Transactional
    public void logout(String rawRefreshToken) {
        if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
            return;
        }
        String tokenHash = refreshTokenGenerator.hash(rawRefreshToken);
        refreshTokenRepository.findByTokenHashForUpdate(tokenHash)
            .ifPresent(token -> token.revoke(now()));
    }

    private LoginSession issueLoginSession(AppUser user, boolean newUser, LocalDateTime issuedAt) {
        IssuedRefreshToken refreshToken = issueRefreshToken(user, issuedAt);
        return new LoginSession(
            accessTokenService.issue(user.getId()),
            accessTokenService.expiresInSeconds(),
            refreshToken.rawToken(),
            user.getId(),
            newUser,
            user.isProfileCompleted()
        );
    }

    private IssuedRefreshToken issueRefreshToken(AppUser user, LocalDateTime issuedAt) {
        RefreshTokenGenerator.RefreshTokenMaterial material = refreshTokenGenerator.generate();
        refreshTokenRepository.save(new RefreshToken(
            user,
            material.tokenHash(),
            issuedAt.plus(properties.refreshTokenExpiration()),
            issuedAt
        ));
        return new IssuedRefreshToken(material.rawToken());
    }

    private LocalDateTime now() {
        return LocalDateTime.ofInstant(clock.instant(), ZoneOffset.UTC);
    }

    private record IssuedRefreshToken(String rawToken) {
        @Override
        public String toString() {
            return "IssuedRefreshToken[REDACTED]";
        }
    }

    public static final class LoginSession {
        private final String accessToken;
        private final long expiresIn;
        private final String refreshToken;
        private final Long userId;
        private final boolean newUser;
        private final boolean profileCompleted;

        private LoginSession(
            String accessToken,
            long expiresIn,
            String refreshToken,
            Long userId,
            boolean newUser,
            boolean profileCompleted
        ) {
            this.accessToken = accessToken;
            this.expiresIn = expiresIn;
            this.refreshToken = refreshToken;
            this.userId = userId;
            this.newUser = newUser;
            this.profileCompleted = profileCompleted;
        }

        public String accessToken() {
            return accessToken;
        }

        public long expiresIn() {
            return expiresIn;
        }

        public String refreshToken() {
            return refreshToken;
        }

        public Long userId() {
            return userId;
        }

        public boolean isNewUser() {
            return newUser;
        }

        public boolean isProfileCompleted() {
            return profileCompleted;
        }

        @Override
        public String toString() {
            return "LoginSession[userId=" + userId + ", newUser=" + newUser
                + ", profileCompleted=" + profileCompleted + ", tokens=REDACTED]";
        }
    }

    public static final class RefreshSession {
        private final String accessToken;
        private final long expiresIn;
        private final String refreshToken;

        private RefreshSession(String accessToken, long expiresIn, String refreshToken) {
            this.accessToken = accessToken;
            this.expiresIn = expiresIn;
            this.refreshToken = refreshToken;
        }

        public String accessToken() {
            return accessToken;
        }

        public long expiresIn() {
            return expiresIn;
        }

        public String refreshToken() {
            return refreshToken;
        }

        @Override
        public String toString() {
            return "RefreshSession[tokens=REDACTED]";
        }
    }
}
