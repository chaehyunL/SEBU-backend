package com.sebu.backend.api.laboratory;

import com.sebu.backend.domain.laboratory.RecruitmentStatus;
import java.util.List;

public record LaboratoriesResponse(List<LaboratoryResponse> laboratories) {
    public record LaboratoryResponse(
        Long id, String name, String websiteUrl, ProfessorResponse professor,
        CollegeResponse college, DepartmentResponse department, List<String> researchFields,
        RecruitmentStatus recruitmentStatus, long bookmarkCount, boolean bookmarked) { }
    public record ProfessorResponse(Long id, String name, String email) { }
    public record CollegeResponse(Long id, String name) { }
    public record DepartmentResponse(Long id, String name) { }
}
