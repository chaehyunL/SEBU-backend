package com.sebu.backend.auth.dto;

import com.sebu.backend.auth.service.CurrentUserService;

public record MeResponse(Long id, String nickname, boolean profileCompleted) {
    public static MeResponse from(CurrentUserService.CurrentUser user) {
        return new MeResponse(user.id(), user.nickname(), user.profileCompleted());
    }
}
