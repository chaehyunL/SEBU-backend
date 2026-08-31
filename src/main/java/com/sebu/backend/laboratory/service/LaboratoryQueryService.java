package com.sebu.backend.laboratory.service;

import com.sebu.backend.global.auth.CurrentUserProvider;
import com.sebu.backend.laboratory.dto.LaboratoriesPagedResult;
import com.sebu.backend.laboratory.dto.LaboratoriesResult;
import com.sebu.backend.laboratory.dto.LaboratoriesResult.AffiliationResult;
import com.sebu.backend.laboratory.dto.LaboratoriesResult.LaboratoryResult;
import com.sebu.backend.laboratory.dto.LaboratoriesResult.ResearchFieldCategoryResult;
import com.sebu.backend.laboratory.query.LaboratorySummaryAssembler;
import com.sebu.backend.laboratory.repository.LaboratoryAffiliationProjection;
import com.sebu.backend.laboratory.repository.LaboratoryDepartmentRepository;
import com.sebu.backend.laboratory.repository.LaboratoryRepository;
import com.sebu.backend.laboratory.repository.LaboratoryResearchFieldCategoryProjection;
import com.sebu.backend.laboratory.repository.LaboratoryResearchFieldCategoryQueryRepository;
import com.sebu.backend.laboratory.repository.LaboratoryResearchFieldProjection;
import com.sebu.backend.laboratory.repository.LaboratoryResearchFieldRepository;
import com.sebu.backend.laboratory.repository.LaboratorySummaryProjection;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
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
    private final LaboratoryResearchFieldCategoryQueryRepository
            laboratoryResearchFieldCategoryQueryRepository;
    private final CurrentUserProvider currentUserProvider;
    private final LaboratorySummaryAssembler laboratorySummaryAssembler;

    @Transactional(readOnly = true)
    public LaboratoriesResult getAll() {
        Long userId = currentUserProvider.currentUserId()
                .orElse(null);

        List<LaboratorySummaryProjection> summaries =
                laboratoryRepository.findAllSummaries(userId);

        if (summaries.isEmpty()) {
            return new LaboratoriesResult(List.of());
        }

        List<LaboratoryResult> laboratories =
                assembleLaboratories(summaries);

        return new LaboratoriesResult(laboratories);
    }

    @Transactional(readOnly = true)
    public LaboratoriesPagedResult getAllByReviewCount(
            int page,
            int size
    ) {
        Long userId = currentUserProvider.currentUserId()
                .orElse(null);

        Page<LaboratorySummaryProjection> summaryPage =
                laboratoryRepository.findAllSummariesByReviewCount(
                        userId,
                        PageRequest.of(page, size)
                );

        List<LaboratorySummaryProjection> summaries =
                summaryPage.getContent();

        List<LaboratoryResult> laboratories =
                summaries.isEmpty()
                        ? List.of()
                        : assembleLaboratories(summaries);

        return new LaboratoriesPagedResult(
                laboratories,
                summaryPage.getNumber(),
                summaryPage.getSize(),
                summaryPage.getTotalElements(),
                summaryPage.hasNext()
        );
    }

    private List<LaboratoryResult> assembleLaboratories(
            List<LaboratorySummaryProjection> summaries
    ) {
        List<Long> laboratoryIds =
                laboratoryIds(summaries);

        Map<Long, List<String>> researchFields =
                findResearchFields(laboratoryIds);

        Map<Long, List<ResearchFieldCategoryResult>>
                researchFieldCategories =
                findResearchFieldCategories(laboratoryIds);

        Map<Long, List<AffiliationResult>> affiliations =
                findAffiliations(laboratoryIds);

        return summaries.stream()
                .map(summary ->
                        laboratorySummaryAssembler.assemble(
                                summary,
                                researchFields.getOrDefault(
                                        summary.getId(),
                                        List.of()
                                ),
                                researchFieldCategories.getOrDefault(
                                        summary.getId(),
                                        List.of()
                                ),
                                affiliations.getOrDefault(
                                        summary.getId(),
                                        List.of()
                                )
                        )
                )
                .toList();
    }

    private List<Long> laboratoryIds(
            List<LaboratorySummaryProjection> summaries
    ) {
        return summaries.stream()
                .map(LaboratorySummaryProjection::getId)
                .distinct()
                .toList();
    }

    private Map<Long, List<String>> findResearchFields(
            List<Long> laboratoryIds
    ) {
        return laboratoryResearchFieldRepository
                .findFieldsByLaboratoryIds(laboratoryIds)
                .stream()
                .collect(
                        Collectors.groupingBy(
                                LaboratoryResearchFieldProjection::getLaboratoryId,
                                LinkedHashMap::new,
                                Collectors.mapping(
                                        LaboratoryResearchFieldProjection::getName,
                                        Collectors.toList()
                                )
                        )
                );
    }

    private Map<Long, List<ResearchFieldCategoryResult>>
    findResearchFieldCategories(
            List<Long> laboratoryIds
    ) {
        Map<Long, LinkedHashMap<Long, ResearchFieldCategoryResult>>
                categoriesByLaboratory =
                new LinkedHashMap<>();

        for (LaboratoryResearchFieldCategoryProjection category
                : laboratoryResearchFieldCategoryQueryRepository
                .findAllByLaboratoryIds(laboratoryIds)) {

            categoriesByLaboratory
                    .computeIfAbsent(
                            category.getLaboratoryId(),
                            ignored -> new LinkedHashMap<>()
                    )
                    .computeIfAbsent(
                            category.getCategoryId(),
                            ignored -> toCategoryResult(category)
                    );
        }

        Map<Long, List<ResearchFieldCategoryResult>> results =
                new LinkedHashMap<>();

        categoriesByLaboratory.forEach(
                (laboratoryId, categories) ->
                        results.put(
                                laboratoryId,
                                List.copyOf(categories.values())
                        )
        );

        return results;
    }

    private ResearchFieldCategoryResult toCategoryResult(
            LaboratoryResearchFieldCategoryProjection category
    ) {
        return new ResearchFieldCategoryResult(
                category.getCategoryId(),
                category.getCategoryCode(),
                category.getCategoryName()
        );
    }

    private Map<Long, List<AffiliationResult>> findAffiliations(
            List<Long> laboratoryIds
    ) {
        return laboratoryDepartmentRepository
                .findAffiliationsByLaboratoryIds(laboratoryIds)
                .stream()
                .collect(
                        Collectors.groupingBy(
                                LaboratoryAffiliationProjection::getLaboratoryId,
                                LinkedHashMap::new,
                                Collectors.mapping(
                                        this::toAffiliationResult,
                                        Collectors.toList()
                                )
                        )
                );
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
