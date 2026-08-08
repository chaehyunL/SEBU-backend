package com.sebu.backend.domain.laboratory;

import com.sebu.backend.api.laboratory.LaboratoriesResponse;
import com.sebu.backend.api.laboratory.LaboratoriesResponse.*;
import com.sebu.backend.auth.CurrentUserProvider;
import com.sebu.backend.domain.researchfield.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LaboratoryQueryService {
    private final LaboratoryRepository laboratoryRepository;
    private final LaboratoryResearchFieldRepository laboratoryResearchFieldRepository;
    private final CurrentUserProvider currentUserProvider;

    @Transactional(readOnly = true)
    public LaboratoriesResponse getAll() {
        Long userId = currentUserProvider.currentUserId().orElse(null);
        List<LaboratorySummaryProjection> summaries = laboratoryRepository.findAllSummaries(userId);
        if (summaries.isEmpty()) return new LaboratoriesResponse(List.of());

        List<Long> ids = summaries.stream().map(LaboratorySummaryProjection::getId).toList();
        Map<Long, List<String>> fields = laboratoryResearchFieldRepository.findFieldsByLaboratoryIds(ids).stream()
            .collect(Collectors.groupingBy(LaboratoryResearchFieldProjection::getLaboratoryId,
                LinkedHashMap::new, Collectors.mapping(LaboratoryResearchFieldProjection::getName, Collectors.toList())));

        List<LaboratoryResponse> laboratories = summaries.stream().map(s -> new LaboratoryResponse(
            s.getId(), s.getName(), s.getWebsiteUrl(),
            new ProfessorResponse(s.getProfessorId(), s.getProfessorName(), s.getProfessorEmail()),
            new CollegeResponse(s.getCollegeId(), s.getCollegeName()),
            new DepartmentResponse(s.getDepartmentId(), s.getDepartmentName()),
            fields.getOrDefault(s.getId(), List.of()), s.getRecruitmentStatus(),
            s.getBookmarkCount(), Boolean.TRUE.equals(s.getBookmarked())
        )).toList();
        return new LaboratoriesResponse(laboratories);
    }
}
