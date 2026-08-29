package com.sebu.backend.laboratory.dto;

import com.sebu.backend.laboratory.dto.LaboratoriesResult;
import com.sebu.backend.laboratory.domain.RecruitmentStatus;
import com.sebu.backend.laboratory.domain.LaboratoryNameSource;

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
        LaboratoryNameSource nameSource,
        String websiteUrl,
        ProfessorResponse professor,
        CollegeResponse college,
        DepartmentResponse department,
        List<AffiliationResponse> affiliations,
        List<String> researchFields,
        List<Long> researchFieldCategoryIds,
        List<ResearchFieldCategoryResponse> researchFieldCategories,
        RecruitmentStatus recruitmentStatus,
        long bookmarkCount,
        boolean bookmarked
    ) {
        private static LaboratoryResponse from(LaboratoriesResult.LaboratoryResult result) {
            return new LaboratoryResponse(
                result.id(),
                result.name(),
                result.nameSource(),
                result.websiteUrl(),
                new ProfessorResponse(result.professor().id(), result.professor().name(), result.professor().email()),
                new CollegeResponse(result.college().id(), result.college().name()),
                new DepartmentResponse(result.department().id(), result.department().name()),
                result.affiliations().stream()
                    .map(AffiliationResponse::from)
                    .toList(),
                result.researchFields(),
                result.researchFieldCategories().stream()
                    .map(LaboratoriesResult.ResearchFieldCategoryResult::id)
                    .toList(),
                result.researchFieldCategories().stream()
                    .map(ResearchFieldCategoryResponse::from)
                    .toList(),
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

    public record ResearchFieldCategoryResponse(
        Long id,
        String code,
        String name
    ) {
        private static ResearchFieldCategoryResponse from(
            LaboratoriesResult.ResearchFieldCategoryResult category
        ) {
            return new ResearchFieldCategoryResponse(
                category.id(),
                category.code(),
                category.name()
            );
        }
    }

    public record AffiliationResponse(
        CollegeResponse college,
        DepartmentResponse department
    ) {
        private static AffiliationResponse from(
            LaboratoriesResult.AffiliationResult affiliation
        ) {
            return new AffiliationResponse(
                new CollegeResponse(
                    affiliation.college().id(),
                    affiliation.college().name()
                ),
                new DepartmentResponse(
                    affiliation.department().id(),
                    affiliation.department().name()
                )
            );
        }
    }
}
