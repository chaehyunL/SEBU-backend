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
        this(professor, department, name, websiteUrl, null, recruitmentStatus);
    }

    public Laboratory(
        Professor professor,
        Department department,
        String name,
        String websiteUrl,
        String description,
        RecruitmentStatus recruitmentStatus
    ) {
        validateProfessorDepartment(professor, department);
        this.professor = professor;
        this.department = department;
        this.name = name;
        this.websiteUrl = websiteUrl;
        this.description = description;
        this.recruitmentStatus = recruitmentStatus;
    }

    public void softDelete() {
        deletedAt = LocalDateTime.now();
    }

    public boolean isDeleted() {
        return deletedAt != null;
    }

    private void validateProfessorDepartment(Professor professor, Department department) {
        Department professorDepartment = professor.getDepartment();
        boolean sameEntity = professorDepartment == department;
        boolean samePersistedEntity = professorDepartment.getId() != null
            && Objects.equals(professorDepartment.getId(), department.getId());

        if (!sameEntity && !samePersistedEntity) {
            throw new IllegalArgumentException("PROFESSOR_DEPARTMENT_MISMATCH");
        }
    }
}
