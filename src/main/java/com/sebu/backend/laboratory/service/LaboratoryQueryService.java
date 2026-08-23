package com.sebu.backend.laboratory.service;

import com.sebu.backend.laboratory.dto.LaboratoriesResult;
import com.sebu.backend.laboratory.dto.LaboratoriesResult.CollegeResult;
import com.sebu.backend.laboratory.dto.LaboratoriesResult.DepartmentResult;
import com.sebu.backend.laboratory.dto.LaboratoriesResult.LaboratoryResult;
import com.sebu.backend.laboratory.dto.LaboratoriesResult.ProfessorResult;
import com.sebu.backend.global.auth.CurrentUserProvider;
import com.sebu.backend.laboratory.repository.LaboratoryRepository;
import com.sebu.backend.laboratory.repository.LaboratorySummaryProjection;
import com.sebu.backend.laboratory.repository.LaboratoryResearchFieldProjection;
import com.sebu.backend.laboratory.repository.LaboratoryResearchFieldRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LaboratoryQueryService {
    private final LaboratoryRepository laboratoryRepository;
    private final LaboratoryResearchFieldRepository laboratoryResearchFieldRepository;
    private final CurrentUserProvider currentUserProvider;

    @Transactional(readOnly = true)
    public LaboratoriesResult getAll() {
        Long userId = currentUserProvider.currentUserId().orElse(null);
        List<LaboratorySummaryProjection> summaries = laboratoryRepository.findAllSummaries(userId);
        if (summaries.isEmpty()) {
            return new LaboratoriesResult(List.of());
        }

        Map<Long, List<String>> researchFields = findResearchFields(summaries);
        List<LaboratoryResult> laboratories = summaries.stream()
            .map(summary -> toResult(summary, researchFields.getOrDefault(summary.getId(), List.of())))
            .toList();
        return new LaboratoriesResult(laboratories);
    }

    private Map<Long, List<String>> findResearchFields(List<LaboratorySummaryProjection> summaries) {
        List<Long> laboratoryIds = summaries.stream()
            .map(LaboratorySummaryProjection::getId)
            .toList();
        return laboratoryResearchFieldRepository.findFieldsByLaboratoryIds(laboratoryIds).stream()
            .collect(Collectors.groupingBy(
                LaboratoryResearchFieldProjection::getLaboratoryId,
                LinkedHashMap::new,
                Collectors.mapping(LaboratoryResearchFieldProjection::getName, Collectors.toList())
            ));
    }

    private LaboratoryResult toResult(LaboratorySummaryProjection summary, List<String> researchFields) {
        return new LaboratoryResult(
            summary.getId(),
            summary.getName(),
            summary.getNameSource(),
            summary.getWebsiteUrl(),
            new ProfessorResult(summary.getProfessorId(), summary.getProfessorName(), summary.getProfessorEmail()),
            new CollegeResult(summary.getCollegeId(), summary.getCollegeName()),
            new DepartmentResult(summary.getDepartmentId(), summary.getDepartmentName()),
            researchFields,
            summary.getRecruitmentStatus(),
            summary.getBookmarkCount(),
            Boolean.TRUE.equals(summary.getBookmarked())
        );
    }
}
