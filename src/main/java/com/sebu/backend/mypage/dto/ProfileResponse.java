package com.sebu.backend.mypage.dto;

import com.sebu.backend.user.domain.GpaBand;

import java.time.LocalDateTime;

public record ProfileResponse(
        String name,
        Short grade,
        Major major,
        GpaBand gpaBand,
        String introduction,
        boolean profileCompleted,
        LocalDateTime profileUpdatedAt
) {
    public record Major(
            String id,
            String name
    ) {
    }
}
