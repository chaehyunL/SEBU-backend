package com.sebu.backend.api.laboratory;

import com.sebu.backend.application.laboratory.LaboratoriesResult;
import com.sebu.backend.domain.laboratory.RecruitmentStatus;

import java.util.List;

public record LaboratoriesResponse(List<LaboratoryResponse> laboratories) {
    public static LaboratoriesResponse from(LaboratoriesResult result) {
        List<LaboratoryResponse> laboratories = result.laboratories().stream()
            .map(LaboratoryResponse::from)
            .toList();
        return new LaboratoriesResponse(laboratories);
    }

    public record LaboratoryResponse(
        Long id,
        String name,
        String websiteUrl,
        ProfessorResponse professor,
        CollegeResponse college,
        DepartmentResponse department,
        List<String> researchFields,
        RecruitmentStatus recruitmentStatus,
        long bookmarkCount,
        boolean bookmarked
    ) {
        private static LaboratoryResponse from(LaboratoriesResult.LaboratoryResult result) {
            return new LaboratoryResponse(
                result.id(),
                result.name(),
                result.websiteUrl(),
                new ProfessorResponse(result.professor().id(), result.professor().name(), result.professor().email()),
                new CollegeResponse(result.college().id(), result.college().name()),
                new DepartmentResponse(result.department().id(), result.department().name()),
                result.researchFields(),
                result.recruitmentStatus(),
                result.bookmarkCount(),
                result.bookmarked()
            );
        }
    }

    public record ProfessorResponse(Long id, String name, String email) {
    }

    public record CollegeResponse(Long id, String name) {
    }

    public record DepartmentResponse(Long id, String name) {
    }
}
