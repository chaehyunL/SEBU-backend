package com.sebu.backend.community.profile.controller;

import com.sebu.backend.community.profile.dto.CommunityProfileResponse;
import com.sebu.backend.community.profile.service.CommunityProfileQueryService;
import com.sebu.backend.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(
        name = "커뮤니티 프로필",
        description = "사용자의 커뮤니티 활동 프로필 조회 API"
)
@RestController
@RequiredArgsConstructor
public class CommunityProfileController {
    private final CommunityProfileQueryService profileQueryService;

    @Operation(
            summary = "커뮤니티 프로필 조회",
            description = "사용자 ID와 페이지 정보로 커뮤니티 활동 프로필을 조회합니다."
    )
    @GetMapping("/api/v1/users/{userId}/community-profile")
    public ApiResponse<CommunityProfileResponse> findProfile(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ApiResponse.success(profileQueryService.findProfile(userId, page, size));
    }
}
