package com.sebu.backend.laboratory.query;

import com.sebu.backend.laboratory.dto.LaboratoriesResult;
import com.sebu.backend.laboratory.repository.LaboratorySummaryProjection;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class LaboratorySummaryAssembler {

    public LaboratoriesResult.LaboratoryResult assemble(
            LaboratorySummaryProjection projection,
            List<String> researchFields
    ) {
        return assemble(
            projection,
            researchFields,
            List.of(),
            List.of(primaryAffiliation(projection))
        );
    }

    public LaboratoriesResult.LaboratoryResult assemble(
        LaboratorySummaryProjection projection,
        List<String> researchFields,
        List<LaboratoriesResult.AffiliationResult> affiliations
    ) {
        return assemble(
            projection,
            researchFields,
            List.of(),
            affiliations
        );
    }

    public LaboratoriesResult.LaboratoryResult assemble(
        LaboratorySummaryProjection projection,
        List<String> researchFields,
        List<LaboratoriesResult.ResearchFieldCategoryResult> researchFieldCategories,
        List<LaboratoriesResult.AffiliationResult> affiliations
    ) {
        List<LaboratoriesResult.AffiliationResult> resolvedAffiliations =
            affiliations.isEmpty()
                ? List.of(primaryAffiliation(projection))
                : List.copyOf(affiliations);

        return new LaboratoriesResult.LaboratoryResult(
                projection.getId(),
                projection.getName(),
                projection.getNameSource(),
                projection.getWebsiteUrl(),

                new LaboratoriesResult.ProfessorResult(
                        projection.getProfessorId(),
                        projection.getProfessorName(),
                        projection.getProfessorEmail()
                ),

                new LaboratoriesResult.CollegeResult(
                        projection.getCollegeId(),
                        projection.getCollegeName()
                ),

                new LaboratoriesResult.DepartmentResult(
                        projection.getDepartmentId(),
                        projection.getDepartmentName()
                ),

                resolvedAffiliations,
                researchFields,
                researchFieldCategories,
                projection.getRecruitmentStatus(),
                projection.getBookmarkCount(),
                projection.getBookmarked()
        );
    }

    private LaboratoriesResult.AffiliationResult primaryAffiliation(
        LaboratorySummaryProjection projection
    ) {
        return new LaboratoriesResult.AffiliationResult(
            new LaboratoriesResult.CollegeResult(
                projection.getCollegeId(),
                projection.getCollegeName()
            ),
            new LaboratoriesResult.DepartmentResult(
                projection.getDepartmentId(),
                projection.getDepartmentName()
            )
        );
    }
}
