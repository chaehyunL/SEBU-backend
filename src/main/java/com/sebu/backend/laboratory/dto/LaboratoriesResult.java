package com.sebu.backend.laboratory.dto;

import com.sebu.backend.laboratory.domain.RecruitmentStatus;

import java.util.List;

public record LaboratoriesResult(List<LaboratoryResult> laboratories) {
    public record LaboratoryResult(
        Long id,
        String name,
        String websiteUrl,
        ProfessorResult professor,
        CollegeResult college,
        DepartmentResult department,
        List<String> researchFields,
        RecruitmentStatus recruitmentStatus,
        long bookmarkCount,
        boolean bookmarked
    ) {
    }

    public record ProfessorResult(Long id, String name, String email) {
    }

    public record CollegeResult(Long id, String name) {
    }

    public record DepartmentResult(Long id, String name) {
    }
}
