package com.sebu.backend.researchfield.category.service;

import com.sebu.backend.researchfield.category.domain.ResearchFieldCategory;
import com.sebu.backend.researchfield.category.repository.ResearchFieldCategoryRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ResearchFieldCategoryQueryServiceTest {
    @Mock
    ResearchFieldCategoryRepository researchFieldCategoryRepository;

    @Mock
    ResearchFieldCategory firstCategory;

    @Mock
    ResearchFieldCategory secondCategory;

    @InjectMocks
    ResearchFieldCategoryQueryService service;

    @Test
    void returnsRepositoryOrderAsCategoryResults() {
        when(researchFieldCategoryRepository.findAllByOrderByDisplayOrderAscIdAsc())
            .thenReturn(List.of(firstCategory, secondCategory));
        mockCategory(
            firstCategory,
            1L,
            "AI_ML",
            "인공지능·기계학습",
            "인공지능과 기계학습",
            1
        );
        mockCategory(
            secondCategory,
            2L,
            "DATA_INFORMATION",
            "데이터과학·정보관리",
            "데이터과학과 정보관리",
            2
        );

        var categories = service.getAll().categories();

        assertThat(categories)
            .extracting(category -> category.code())
            .containsExactly("AI_ML", "DATA_INFORMATION");
        assertThat(categories.getFirst().description())
            .isEqualTo("인공지능과 기계학습");
        assertThat(categories.getFirst().displayOrder()).isEqualTo(1);
    }

    @Test
    void returnsEmptyResultWhenNoCategoryExists() {
        when(researchFieldCategoryRepository.findAllByOrderByDisplayOrderAscIdAsc())
            .thenReturn(List.of());

        assertThat(service.getAll().categories()).isEmpty();
    }

    private void mockCategory(
        ResearchFieldCategory category,
        Long id,
        String code,
        String name,
        String description,
        int displayOrder
    ) {
        when(category.getId()).thenReturn(id);
        when(category.getCode()).thenReturn(code);
        when(category.getName()).thenReturn(name);
        when(category.getDescription()).thenReturn(description);
        when(category.getDisplayOrder()).thenReturn(displayOrder);
    }
}
