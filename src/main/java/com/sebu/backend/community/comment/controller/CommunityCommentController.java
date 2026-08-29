package com.sebu.backend.community.comment.controller;

import com.sebu.backend.auth.exception.AccessTokenInvalidException;
import com.sebu.backend.community.comment.dto.CommentCreateRequest;
import com.sebu.backend.community.comment.dto.CommentCreateResponse;
import com.sebu.backend.community.comment.dto.CommentDeleteResponse;
import com.sebu.backend.community.comment.dto.CommentListResponse;
import com.sebu.backend.community.comment.dto.CommentUpdateRequest;
import com.sebu.backend.community.comment.dto.CommentUpdateResponse;
import com.sebu.backend.community.comment.service.CommunityCommentCommandService;
import com.sebu.backend.community.comment.service.CommunityCommentQueryService;
import com.sebu.backend.global.auth.CurrentUserProvider;
import com.sebu.backend.global.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/posts/{postId}/comments")
public class CommunityCommentController {
    private final CommunityCommentCommandService commandService;
    private final CommunityCommentQueryService queryService;
    private final CurrentUserProvider currentUserProvider;

    @GetMapping
    public ApiResponse<CommentListResponse> findComments(
            @PathVariable Long postId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ApiResponse.success(queryService.findComments(postId, page, size));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<CommentCreateResponse>> create(
            @PathVariable Long postId,
            @Valid @RequestBody CommentCreateRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(commandService.create(
                        requireCurrentUser(),
                        postId,
                        request
                )));
    }

    @PatchMapping("/{commentId}")
    public ApiResponse<CommentUpdateResponse> update(
            @PathVariable Long postId,
            @PathVariable Long commentId,
            @Valid @RequestBody CommentUpdateRequest request
    ) {
        return ApiResponse.success(commandService.update(
                requireCurrentUser(),
                postId,
                commentId,
                request
        ));
    }

    @DeleteMapping("/{commentId}")
    public ApiResponse<CommentDeleteResponse> delete(
            @PathVariable Long postId,
            @PathVariable Long commentId
    ) {
        return ApiResponse.success(commandService.delete(
                requireCurrentUser(),
                postId,
                commentId
        ));
    }

    private Long requireCurrentUser() {
        return currentUserProvider.currentUserId()
                .orElseThrow(AccessTokenInvalidException::new);
    }
}
