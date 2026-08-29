package com.sebu.backend.community.reaction.controller;

import com.sebu.backend.auth.exception.AccessTokenInvalidException;
import com.sebu.backend.community.reaction.dto.PostBookmarkResponse;
import com.sebu.backend.community.reaction.dto.PostLikeResponse;
import com.sebu.backend.community.reaction.service.CommunityPostReactionService;
import com.sebu.backend.global.auth.CurrentUserProvider;
import com.sebu.backend.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/posts/{postId}")
public class CommunityPostReactionController {
    private final CommunityPostReactionService reactionService;
    private final CurrentUserProvider currentUserProvider;

    @PutMapping("/likes")
    public ApiResponse<PostLikeResponse> like(@PathVariable Long postId) {
        return ApiResponse.success(reactionService.like(requireCurrentUser(), postId));
    }

    @DeleteMapping("/likes")
    public ApiResponse<PostLikeResponse> unlike(@PathVariable Long postId) {
        return ApiResponse.success(reactionService.unlike(requireCurrentUser(), postId));
    }

    @PutMapping("/bookmarks")
    public ApiResponse<PostBookmarkResponse> bookmark(@PathVariable Long postId) {
        return ApiResponse.success(reactionService.bookmark(requireCurrentUser(), postId));
    }

    @DeleteMapping("/bookmarks")
    public ApiResponse<PostBookmarkResponse> unbookmark(@PathVariable Long postId) {
        return ApiResponse.success(reactionService.unbookmark(requireCurrentUser(), postId));
    }

    private Long requireCurrentUser() {
        return currentUserProvider.currentUserId()
                .orElseThrow(AccessTokenInvalidException::new);
    }
}
