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
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(name = "app_user")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AppUser extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 255)
    private String email;

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
        this.email = email.trim().toLowerCase();
    }
}
