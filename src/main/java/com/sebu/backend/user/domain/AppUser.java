package com.sebu.backend.user.domain;

import com.sebu.backend.department.domain.Department;
import com.sebu.backend.global.domain.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
import java.util.Objects;

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

    @Column(length = 30)
    private String name;

    private Short grade;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "major_department_id")
    private Department majorDepartment;

    @Enumerated(EnumType.STRING)
    @Column(name = "gpa_band", length = 20)
    private GpaBand gpaBand;

    @Column(nullable = false, length = 500)
    private String introduction = "";

    @Column(name = "introduction_moderated_at")
    private LocalDateTime introductionModeratedAt;

    @Column(name = "introduction_policy_version", length = 50)
    private String introductionPolicyVersion;

    @Column(name = "introduction_provider_version", length = 100)
    private String introductionProviderVersion;

    @Column(name = "profile_updated_at")
    private LocalDateTime profileUpdatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

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

    public void updateProfile(
            String name,
            Short grade,
            Department majorDepartment,
            GpaBand gpaBand,
            String introduction,
            LocalDateTime moderatedAt,
            String policyVersion,
            String providerVersion
    ) {
        boolean changed =
                !Objects.equals(this.name, name)
                        || !Objects.equals(this.grade, grade)
                        || !Objects.equals(this.majorDepartment, majorDepartment)
                        || !Objects.equals(this.gpaBand, gpaBand)
                        || !Objects.equals(this.introduction, introduction);

        this.name = name;
        this.grade = grade;
        this.majorDepartment = majorDepartment;
        this.gpaBand = gpaBand;
        this.introduction = introduction;

        this.introductionModeratedAt = moderatedAt;
        this.introductionPolicyVersion = policyVersion;
        this.introductionProviderVersion = providerVersion;

        this.profileCompleted =
                name != null
                        && grade != null
                        && majorDepartment != null;

        if (changed) {
            this.profileUpdatedAt = LocalDateTime.now();
        }
    }

    public void withdraw() {
        if (this.deletedAt == null) {
            this.deletedAt = LocalDateTime.now();
        }
    }

    public boolean isDeleted() {
        return this.deletedAt != null;
    }
}
