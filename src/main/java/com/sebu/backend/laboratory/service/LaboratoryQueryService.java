package com.sebu.backend.laboratory.service;

import com.sebu.backend.laboratory.dto.LaboratoriesResult;
import com.sebu.backend.laboratory.dto.LaboratoriesResult.AffiliationResult;
import com.sebu.backend.laboratory.dto.LaboratoriesResult.LaboratoryResult;
import com.sebu.backend.global.auth.CurrentUserProvider;
import com.sebu.backend.laboratory.query.LaboratorySummaryAssembler;
import com.sebu.backend.laboratory.repository.LaboratoryAffiliationProjection;
import com.sebu.backend.laboratory.repository.LaboratoryDepartmentRepository;
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
    private final LaboratoryDepartmentRepository laboratoryDepartmentRepository;
    private final LaboratoryResearchFieldRepository laboratoryResearchFieldRepository;
    private final CurrentUserProvider currentUserProvider;
    private final LaboratorySummaryAssembler laboratorySummaryAssembler;

    @Transactional(readOnly = true)
    public LaboratoriesResult getAll() {
        Long userId = currentUserProvider.currentUserId().orElse(null);
        List<LaboratorySummaryProjection> summaries = laboratoryRepository.findAllSummaries(userId);
        if (summaries.isEmpty()) {
            return new LaboratoriesResult(List.of());
        }

        Map<Long, List<String>> researchFields = findResearchFields(summaries);
        Map<Long, List<AffiliationResult>> affiliations = findAffiliations(summaries);
        List<LaboratoryResult> laboratories = summaries.stream()
            .map(summary -> laboratorySummaryAssembler.assemble(
                summary,
                researchFields.getOrDefault(summary.getId(), List.of()),
                affiliations.getOrDefault(summary.getId(), List.of())
            ))
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

    private Map<Long, List<AffiliationResult>> findAffiliations(
        List<LaboratorySummaryProjection> summaries
    ) {
        List<Long> laboratoryIds = summaries.stream()
            .map(LaboratorySummaryProjection::getId)
            .distinct()
            .toList();

        return laboratoryDepartmentRepository
            .findAffiliationsByLaboratoryIds(laboratoryIds)
            .stream()
            .collect(Collectors.groupingBy(
                LaboratoryAffiliationProjection::getLaboratoryId,
                LinkedHashMap::new,
                Collectors.mapping(this::toAffiliationResult, Collectors.toList())
            ));
    }

    private AffiliationResult toAffiliationResult(
        LaboratoryAffiliationProjection affiliation
    ) {
        return new AffiliationResult(
            new LaboratoriesResult.CollegeResult(
                affiliation.getCollegeId(),
                affiliation.getCollegeName()
            ),
            new LaboratoriesResult.DepartmentResult(
                affiliation.getDepartmentId(),
                affiliation.getDepartmentName()
            )
        );
    }
}
