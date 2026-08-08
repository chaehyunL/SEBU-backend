package com.sebu.backend.api.laboratory;

import com.sebu.backend.api.common.ApiResponse;
import com.sebu.backend.domain.laboratory.LaboratoryQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/laboratories")
@RequiredArgsConstructor
public class LaboratoryController {
    private final LaboratoryQueryService laboratoryQueryService;
    @GetMapping
    public ApiResponse<LaboratoriesResponse> getAll() { return ApiResponse.success(laboratoryQueryService.getAll()); }
}
