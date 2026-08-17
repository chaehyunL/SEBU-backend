package com.sebu.backend.laboratory.controller;

import com.sebu.backend.laboratory.dto.LaboratoriesResponse;
import com.sebu.backend.global.response.ApiResponse;
import com.sebu.backend.laboratory.service.LaboratoryQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/laboratories")
@RequiredArgsConstructor
public class LaboratoryController {
    private final LaboratoryQueryService laboratoryQueryService;
    @GetMapping
    public ApiResponse<LaboratoriesResponse> getAll() {
        return ApiResponse.success(LaboratoriesResponse.from(laboratoryQueryService.getAll()));
    }
}
