package com.sebu.backend.community.comment.dto;

import com.sebu.backend.community.common.dto.CommunityAuthorResponse;

import java.time.LocalDateTime;
import java.util.List;

public record CommentListResponse(
        List<CommentItem> comments,
        int page,
        int size,
        long totalElements,
        boolean hasNext
) {
    public record CommentItem(
            Long id,
            CommunityAuthorResponse author,
            String content,
            boolean mine,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
    }
}
