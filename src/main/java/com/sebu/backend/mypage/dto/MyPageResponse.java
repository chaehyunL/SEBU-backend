package com.sebu.backend.mypage.dto;

import com.sebu.backend.user.domain.GpaBand;

import java.time.LocalDateTime;
import java.util.List;

public record MyPageResponse(
        Profile profile,
        Summary summary,
        BookmarkedLaboratories bookmarkedLaboratories
) {
    public record Profile(
            String name,
            String nickname,
            Short grade,
            DepartmentSummary department,
            GpaBand gpaBand,
            String introduction,
            boolean profileCompleted,
            LocalDateTime profileUpdatedAt
    ) {
    }

    public record Summary(
            long bookmarkedLaboratoryCount
    ) {
    }

    public record BookmarkedLaboratories(
            List<BookmarkedLaboratory> items,
            boolean hasNext
    ) {
    }

    public record BookmarkedLaboratory(
            LocalDateTime bookmarkedAt,
            LaboratorySummary laboratory
    ) {
    }

    public record LaboratorySummary(
            String id,
            String name,
            String websiteUrl,
            CollegeSummary college,
            DepartmentSummary department,
            ProfessorSummary professor,
            List<String> researchFields,
            String recruitmentStatus,
            long bookmarkCount,
            boolean bookmarked
    ) {
    }

    public record CollegeSummary(
            String id,
            String name
    ) {
    }

    public record DepartmentSummary(
            String id,
            String name
    ) {
    }

    public record ProfessorSummary(
            String id,
            String name
    ) {
    }
}
