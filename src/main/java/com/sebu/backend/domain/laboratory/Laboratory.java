package com.sebu.backend.domain.laboratory;

import com.sebu.backend.domain.common.BaseTimeEntity;
import com.sebu.backend.domain.department.Department;
import com.sebu.backend.domain.professor.Professor;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Getter @Entity @Table(name = "laboratory") @NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Laboratory extends BaseTimeEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "professor_id", nullable = false) private Professor professor;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "department_id", nullable = false) private Department department;
    @Column(nullable = false, length = 150) private String name;
    @Column(name = "website_url", length = 2048) private String websiteUrl;
    @Enumerated(EnumType.STRING) @Column(name = "recruitment_status", nullable = false, length = 30) private RecruitmentStatus recruitmentStatus;
    @Column(name = "deleted_at") private LocalDateTime deletedAt;
    public Laboratory(Professor professor, Department department, String name, String websiteUrl, RecruitmentStatus recruitmentStatus) {
        this.professor = professor; this.department = department; this.name = name; this.websiteUrl = websiteUrl; this.recruitmentStatus = recruitmentStatus;
    }
    public void softDelete() { deletedAt = LocalDateTime.now(); }
    public boolean isDeleted() { return deletedAt != null; }
}
