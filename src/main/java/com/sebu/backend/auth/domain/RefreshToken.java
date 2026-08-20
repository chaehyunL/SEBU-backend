package com.sebu.backend.auth.domain;

import com.sebu.backend.user.domain.AppUser;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(
    name = "refresh_token",
    uniqueConstraints = @UniqueConstraint(name = "uk_refresh_token_hash", columnNames = "token_hash")
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RefreshToken {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    @Column(name = "token_hash", nullable = false, length = 64)
    private String tokenHash;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "revoked_at")
    private LocalDateTime revokedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public RefreshToken(AppUser user, String tokenHash, LocalDateTime expiresAt, LocalDateTime createdAt) {
        if (user == null) {
            throw new IllegalArgumentException("REFRESH_TOKEN_USER_REQUIRED");
        }
        if (tokenHash == null || tokenHash.isBlank()) {
            throw new IllegalArgumentException("REFRESH_TOKEN_HASH_REQUIRED");
        }
        if (tokenHash.length() != 64) {
            throw new IllegalArgumentException("REFRESH_TOKEN_HASH_INVALID");
        }
        if (createdAt == null || expiresAt == null || !expiresAt.isAfter(createdAt)) {
            throw new IllegalArgumentException("REFRESH_TOKEN_EXPIRATION_INVALID");
        }
        this.user = user;
        this.tokenHash = tokenHash;
        this.expiresAt = expiresAt;
        this.createdAt = createdAt;
    }

    public boolean isUsableAt(LocalDateTime now) {
        return revokedAt == null && now.isBefore(expiresAt);
    }

    public void revoke(LocalDateTime now) {
        if (revokedAt == null) {
            revokedAt = now;
        }
    }
}
