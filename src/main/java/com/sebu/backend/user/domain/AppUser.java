package com.sebu.backend.user.domain;

import com.sebu.backend.global.domain.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
    name = "app_user",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_app_user_provider_identity",
        columnNames = {"provider", "provider_user_id"}
    )
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AppUser extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, length = 255)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private AuthProvider provider;

    @Column(name = "provider_user_id", length = 100)
    private String providerUserId;

    @Column(name = "profile_completed", nullable = false)
    private boolean profileCompleted;

    public AppUser(String email) {
        this.email = normalizeEmail(email);
    }

    private AppUser(AuthProvider provider, String providerUserId) {
        this.provider = provider;
        this.providerUserId = requireProviderUserId(providerUserId);
    }

    public static AppUser sejong(String providerUserId) {
        return new AppUser(AuthProvider.SEJONG, providerUserId);
    }

    private static String normalizeEmail(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("USER_EMAIL_REQUIRED");
        }
        return value.trim().toLowerCase();
    }

    private static String requireProviderUserId(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("PROVIDER_USER_ID_REQUIRED");
        }
        return value.trim();
    }
}
