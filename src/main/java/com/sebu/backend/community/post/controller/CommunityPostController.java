package com.sebu.backend.community.post.controller;

import com.sebu.backend.auth.exception.AccessTokenInvalidException;
import com.sebu.backend.community.post.domain.CommunityPostCategory;
import com.sebu.backend.community.post.domain.CommunityPostSort;
import com.sebu.backend.community.post.dto.PostCreateRequest;
import com.sebu.backend.community.post.dto.PostCreateResponse;
import com.sebu.backend.community.post.dto.PostDeleteResponse;
import com.sebu.backend.community.post.dto.PostDetailResponse;
import com.sebu.backend.community.post.dto.PostListResponse;
import com.sebu.backend.community.post.dto.PostUpdateRequest;
import com.sebu.backend.community.post.dto.PostUpdateResponse;
import com.sebu.backend.community.post.service.CommunityPostCommandService;
import com.sebu.backend.community.post.service.CommunityPostQueryService;
import com.sebu.backend.global.auth.CurrentUserProvider;
import com.sebu.backend.global.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/posts")
public class CommunityPostController {
    private final CommunityPostCommandService commandService;
    private final CommunityPostQueryService queryService;
    private final CurrentUserProvider currentUserProvider;

    @GetMapping
    public ApiResponse<PostListResponse> findPosts(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) CommunityPostCategory category,
            @RequestParam(defaultValue = "LATEST") CommunityPostSort sort,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ApiResponse.success(queryService.findPosts(keyword, category, sort, page, size));
    }

    @GetMapping("/{postId}")
    public ApiResponse<PostDetailResponse> findDetail(@PathVariable Long postId) {
        return ApiResponse.success(queryService.findDetail(postId));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<PostCreateResponse>> create(
            @Valid @RequestBody PostCreateRequest request
    ) {
        Long userId = requireCurrentUser();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(commandService.create(userId, request)));
    }

    @PutMapping("/{postId}")
    public ApiResponse<PostUpdateResponse> update(
            @PathVariable Long postId,
            @Valid @RequestBody PostUpdateRequest request
    ) {
        return ApiResponse.success(commandService.update(requireCurrentUser(), postId, request));
    }

    @DeleteMapping("/{postId}")
    public ApiResponse<PostDeleteResponse> delete(@PathVariable Long postId) {
        return ApiResponse.success(commandService.delete(requireCurrentUser(), postId));
    }

    private Long requireCurrentUser() {
        return currentUserProvider.currentUserId()
                .orElseThrow(AccessTokenInvalidException::new);
    }
}
