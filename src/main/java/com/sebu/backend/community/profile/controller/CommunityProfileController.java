package com.sebu.backend.community.profile.controller;

import com.sebu.backend.community.profile.dto.CommunityProfileResponse;
import com.sebu.backend.community.profile.service.CommunityProfileQueryService;
import com.sebu.backend.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class CommunityProfileController {
    private final CommunityProfileQueryService profileQueryService;

    @GetMapping("/api/v1/users/{userId}/community-profile")
    public ApiResponse<CommunityProfileResponse> findProfile(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ApiResponse.success(profileQueryService.findProfile(userId, page, size));
    }
}
