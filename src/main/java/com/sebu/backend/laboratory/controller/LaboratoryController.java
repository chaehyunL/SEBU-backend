package com.sebu.backend.laboratory.controller;

import com.sebu.backend.global.response.ApiResponse;
import com.sebu.backend.laboratory.dto.LaboratoriesPagedResponse;
import com.sebu.backend.laboratory.dto.LaboratoriesResponse;
import com.sebu.backend.laboratory.service.LaboratoryQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/laboratories")
@RequiredArgsConstructor
public class LaboratoryController {

    private final LaboratoryQueryService laboratoryQueryService;

    @GetMapping
    public ApiResponse<?> getAll(
            @RequestParam(required = false) String sort,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size
    ) {
        if ("REVIEW_COUNT_DESC".equals(sort)) {
            int resolvedPage = page == null ? 0 : page;
            int resolvedSize = size == null ? 20 : size;

            return ApiResponse.success(
                    LaboratoriesPagedResponse.from(
                            laboratoryQueryService.getAllByReviewCount(
                                    resolvedPage,
                                    resolvedSize
                            )
                    )
            );
        }

        return ApiResponse.success(
                LaboratoriesResponse.from(
                        laboratoryQueryService.getAll()
                )
        );
    }
}
