package com.sebu.backend.community.profile.dto;

import com.sebu.backend.community.post.domain.CommunityPostCategory;

import java.time.LocalDateTime;
import java.util.List;

public record CommunityProfileResponse(
        Profile profile,
        Stats stats,
        Posts posts
) {
    public record Profile(
            Long userId,
            String nickname,
            Short grade,
            MajorDepartment majorDepartment,
            LocalDateTime joinedAt,
            String introduction,
            List<Badge> badges
    ) {
    }

    public record MajorDepartment(
            Long id,
            String name,
            College college
    ) {
    }

    public record College(
            Long id,
            String name
    ) {
    }

    public record Badge(
            String code,
            String label
    ) {
    }

    public record Stats(
            long writtenPostCount,
            long receivedLikeCount,
            long writtenCommentCount
    ) {
    }

    public record Posts(
            List<PostItem> items,
            int page,
            int size,
            long totalElements,
            boolean hasNext
    ) {
    }

    public record PostItem(
            Long id,
            CommunityPostCategory category,
            String title,
            long likeCount,
            long commentCount,
            long viewCount,
            LocalDateTime createdAt
    ) {
    }
}
