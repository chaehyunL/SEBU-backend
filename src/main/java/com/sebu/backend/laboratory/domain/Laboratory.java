package com.sebu.backend.laboratory.domain;

import com.sebu.backend.global.domain.BaseTimeEntity;
import com.sebu.backend.department.domain.Department;
import com.sebu.backend.professor.domain.Professor;
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
import java.util.Objects;

@Getter
@Entity
@Table(name = "laboratory")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Laboratory extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "professor_id", nullable = false)
    private Professor professor;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "department_id", nullable = false)
    private Department department;

    @Column(nullable = false, length = 150)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "name_source", nullable = false, length = 20)
    private LaboratoryNameSource nameSource;

    @Column(name = "website_url", length = 2048)
    private String websiteUrl;

    @Column(length = 2000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "recruitment_status", nullable = false, length = 30)
    private RecruitmentStatus recruitmentStatus;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    public Laboratory(
        Professor professor,
        Department department,
        String name,
        String websiteUrl,
        RecruitmentStatus recruitmentStatus
    ) {
        this(
            professor,
            department,
            name,
            websiteUrl,
            null,
            recruitmentStatus,
            LaboratoryNameSource.OFFICIAL
        );
    }

    public Laboratory(
        Professor professor,
        Department department,
        String name,
        String websiteUrl,
        String description,
        RecruitmentStatus recruitmentStatus
    ) {
        this(
            professor,
            department,
            name,
            websiteUrl,
            description,
            recruitmentStatus,
            LaboratoryNameSource.OFFICIAL
        );
    }

    public Laboratory(
        Professor professor,
        Department department,
        String name,
        String websiteUrl,
        String description,
        RecruitmentStatus recruitmentStatus,
        LaboratoryNameSource nameSource
    ) {
        validateProfessorDepartment(professor, department);
        this.professor = professor;
        this.department = department;
        applyDetails(name, websiteUrl, description, recruitmentStatus, nameSource);
    }

    public boolean hasPromotionDetails(
        String name,
        String websiteUrl,
        String description,
        LaboratoryNameSource nameSource
    ) {
        return Objects.equals(this.name, requireText(name, "LABORATORY_NAME_REQUIRED"))
            && Objects.equals(this.websiteUrl, normalizeNullable(websiteUrl))
            && Objects.equals(this.description, normalizeNullable(description))
            && this.nameSource == Objects.requireNonNull(nameSource, "LABORATORY_NAME_SOURCE_REQUIRED");
    }

    public void updateFromPromotion(
        String name,
        String websiteUrl,
        String description,
        LaboratoryNameSource nameSource
    ) {
        if (isDeleted()) {
            throw new IllegalStateException("DELETED_LABORATORY_CANNOT_BE_PROMOTED");
        }
        this.name = requireText(name, "LABORATORY_NAME_REQUIRED");
        this.websiteUrl = normalizeNullable(websiteUrl);
        this.description = normalizeNullable(description);
        this.nameSource = Objects.requireNonNull(nameSource, "LABORATORY_NAME_SOURCE_REQUIRED");
    }

    public boolean mergeFromPromotion(
        String name,
        String websiteUrl,
        String description,
        LaboratoryNameSource nameSource
    ) {
        if (isDeleted()) {
            throw new IllegalStateException("DELETED_LABORATORY_CANNOT_BE_PROMOTED");
        }
        String normalizedName = requireText(name, "LABORATORY_NAME_REQUIRED");
        String normalizedWebsiteUrl = normalizeNullable(websiteUrl);
        String normalizedDescription = normalizeNullable(description);
        LaboratoryNameSource normalizedNameSource = Objects.requireNonNull(
            nameSource,
            "LABORATORY_NAME_SOURCE_REQUIRED"
        );

        boolean changed = mergeName(normalizedName, normalizedNameSource);
        MergeValue websiteMerge = mergeNullableValue(this.websiteUrl, normalizedWebsiteUrl);
        MergeValue descriptionMerge = mergeNullableValue(this.description, normalizedDescription);
        this.websiteUrl = websiteMerge.value();
        this.description = descriptionMerge.value();
        return changed || websiteMerge.changed() || descriptionMerge.changed();
    }

    public void softDelete() {
        deletedAt = LocalDateTime.now();
    }

    public boolean isDeleted() {
        return deletedAt != null;
    }

    private void validateProfessorDepartment(Professor professor, Department department) {
        Objects.requireNonNull(professor, "PROFESSOR_REQUIRED");
        Objects.requireNonNull(department, "DEPARTMENT_REQUIRED");
        Department professorDepartment = professor.getDepartment();
        boolean sameEntity = professorDepartment == department;
        boolean samePersistedEntity = professorDepartment.getId() != null
            && Objects.equals(professorDepartment.getId(), department.getId());

        if (!sameEntity && !samePersistedEntity) {
            throw new IllegalArgumentException("PROFESSOR_DEPARTMENT_MISMATCH");
        }
    }

    private void applyDetails(
        String name,
        String websiteUrl,
        String description,
        RecruitmentStatus recruitmentStatus,
        LaboratoryNameSource nameSource
    ) {
        this.name = requireText(name, "LABORATORY_NAME_REQUIRED");
        this.websiteUrl = normalizeNullable(websiteUrl);
        this.description = normalizeNullable(description);
        this.recruitmentStatus = Objects.requireNonNull(
            recruitmentStatus,
            "RECRUITMENT_STATUS_REQUIRED"
        );
        this.nameSource = Objects.requireNonNull(nameSource, "LABORATORY_NAME_SOURCE_REQUIRED");
    }

    private boolean mergeName(String requestedName, LaboratoryNameSource requestedSource) {
        if (Objects.equals(name, requestedName)) {
            if (nameSource == LaboratoryNameSource.GENERATED
                && requestedSource == LaboratoryNameSource.OFFICIAL) {
                nameSource = LaboratoryNameSource.OFFICIAL;
                return true;
            }
            return false;
        }
        if (nameSource == LaboratoryNameSource.GENERATED
            && requestedSource == LaboratoryNameSource.OFFICIAL) {
            name = requestedName;
            nameSource = LaboratoryNameSource.OFFICIAL;
            return true;
        }
        if (nameSource == LaboratoryNameSource.OFFICIAL
            && requestedSource == LaboratoryNameSource.GENERATED) {
            return false;
        }
        throw new IllegalStateException("LABORATORY_PROFILE_CONFLICT");
    }

    private MergeValue mergeNullableValue(String current, String requested) {
        if (requested == null || Objects.equals(current, requested)) {
            return new MergeValue(current, false);
        }
        if (current == null) {
            return new MergeValue(requested, true);
        }
        throw new IllegalStateException("LABORATORY_PROFILE_CONFLICT");
    }

    private String requireText(String value, String errorCode) {
        String normalized = normalizeNullable(value);
        if (normalized == null) {
            throw new IllegalArgumentException(errorCode);
        }
        return normalized;
    }

    private String normalizeNullable(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private record MergeValue(String value, boolean changed) {
    }
}
