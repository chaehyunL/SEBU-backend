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

                researchFields,
                projection.getRecruitmentStatus(),
                projection.getBookmarkCount(),
                projection.getBookmarked()
        );
    }
}
