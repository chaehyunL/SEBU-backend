package com.sebu.backend.auth.controller;

import com.sebu.backend.auth.dto.MeResponse;
import com.sebu.backend.auth.service.CurrentUserService;
import com.sebu.backend.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/me")
@RequiredArgsConstructor
public class MeController {
    private final CurrentUserService currentUserService;

    @GetMapping
    public ApiResponse<MeResponse> me() {
        return ApiResponse.success(MeResponse.from(currentUserService.getCurrentUser()));
    }
}
