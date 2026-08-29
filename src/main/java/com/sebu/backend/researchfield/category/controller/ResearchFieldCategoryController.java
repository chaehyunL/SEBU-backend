package com.sebu.backend.researchfield.category.controller;

import com.sebu.backend.global.response.ApiResponse;
import com.sebu.backend.researchfield.category.dto.ResearchFieldCategoriesResponse;
import com.sebu.backend.researchfield.category.service.ResearchFieldCategoryQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/research-field-categories")
@RequiredArgsConstructor
public class ResearchFieldCategoryController {
    private final ResearchFieldCategoryQueryService researchFieldCategoryQueryService;

    @GetMapping
    public ApiResponse<ResearchFieldCategoriesResponse> getAll() {
        return ApiResponse.success(
            ResearchFieldCategoriesResponse.from(
                researchFieldCategoryQueryService.getAll()
            )
        );
    }
}
