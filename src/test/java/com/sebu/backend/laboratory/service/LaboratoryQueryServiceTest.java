package com.sebu.backend.laboratory.service;

import com.sebu.backend.global.auth.CurrentUserProvider;
import com.sebu.backend.laboratory.domain.LaboratoryNameSource;
import com.sebu.backend.laboratory.domain.RecruitmentStatus;
import com.sebu.backend.laboratory.repository.LaboratoryRepository;
import com.sebu.backend.laboratory.repository.LaboratoryResearchFieldRepository;
import com.sebu.backend.laboratory.repository.LaboratorySummaryProjection;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LaboratoryQueryServiceTest {
    @Mock
    LaboratoryRepository laboratoryRepository;

    @Mock
    LaboratoryResearchFieldRepository laboratoryResearchFieldRepository;

    @Mock
    CurrentUserProvider currentUserProvider;

    @Mock
    LaboratorySummaryProjection summary;

    @InjectMocks
    LaboratoryQueryService service;

    @Test
    void mapsGeneratedLaboratoryNameSourceToQueryResult() {
        when(currentUserProvider.currentUserId()).thenReturn(Optional.empty());
        when(laboratoryRepository.findAllSummaries(null)).thenReturn(List.of(summary));
        when(summary.getId()).thenReturn(1L);
        when(summary.getName()).thenReturn("김교수 교수님 연구실");
        when(summary.getNameSource()).thenReturn(LaboratoryNameSource.GENERATED);
        when(summary.getProfessorId()).thenReturn(2L);
        when(summary.getProfessorName()).thenReturn("김교수");
        when(summary.getCollegeId()).thenReturn(3L);
        when(summary.getCollegeName()).thenReturn("인공지능융합대학");
        when(summary.getDepartmentId()).thenReturn(4L);
        when(summary.getDepartmentName()).thenReturn("컴퓨터공학과");
        when(summary.getRecruitmentStatus()).thenReturn(RecruitmentStatus.UNKNOWN);
        when(summary.getBookmarkCount()).thenReturn(0L);
        when(summary.getBookmarked()).thenReturn(false);
        when(laboratoryResearchFieldRepository.findFieldsByLaboratoryIds(List.of(1L)))
            .thenReturn(List.of());

        var laboratory = service.getAll().laboratories().getFirst();

        assertThat(laboratory.name()).isEqualTo("김교수 교수님 연구실");
        assertThat(laboratory.nameSource()).isEqualTo(LaboratoryNameSource.GENERATED);
    }
}
