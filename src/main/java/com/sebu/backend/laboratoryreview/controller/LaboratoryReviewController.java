package com.sebu.backend.laboratoryreview.controller;

import com.sebu.backend.global.auth.CurrentUserProvider;
import com.sebu.backend.global.response.ApiResponse;
import com.sebu.backend.laboratoryreview.dto.LaboratoryReviewCreateRequest;
import com.sebu.backend.laboratoryreview.dto.LaboratoryReviewCreateResponse;
import com.sebu.backend.laboratoryreview.dto.LaboratoryReviewDeleteResponse;
import com.sebu.backend.laboratoryreview.dto.LaboratoryReviewListResponse;
import com.sebu.backend.laboratoryreview.dto.LaboratoryReviewMeResponse;
import com.sebu.backend.laboratoryreview.dto.LaboratoryReviewSummaryResponse;
import com.sebu.backend.laboratoryreview.dto.LaboratoryReviewUpdateRequest;
import com.sebu.backend.laboratoryreview.dto.LaboratoryReviewUpdateResponse;
import com.sebu.backend.laboratoryreview.service.LaboratoryReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/laboratories")
public class LaboratoryReviewController {

    private final LaboratoryReviewService laboratoryReviewService;
    private final CurrentUserProvider currentUserProvider;

    @PostMapping("/{laboratoryId}/reviews")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<LaboratoryReviewCreateResponse> createReview(
            @PathVariable Long laboratoryId,
            @Valid @RequestBody LaboratoryReviewCreateRequest request
    ) {
        Long userId = currentUserProvider.currentUserId()
                .orElseThrow(() ->
                        new IllegalStateException(
                                "AUTHENTICATION_REQUIRED"
                        )
                );

        LaboratoryReviewCreateResponse response =
                laboratoryReviewService.createReview(
                        laboratoryId,
                        userId,
                        request
                );

        return ApiResponse.success(response);
    }

    @GetMapping("/{laboratoryId}/reviews")
    public ApiResponse<LaboratoryReviewListResponse> getReviews(
            @PathVariable Long laboratoryId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Long currentUserId = currentUserProvider.currentUserId()
                .orElse(null);

        LaboratoryReviewListResponse response =
                laboratoryReviewService.getReviews(
                        laboratoryId,
                        currentUserId,
                        page,
                        size
                );

        return ApiResponse.success(response);
    }

    @GetMapping("/{laboratoryId}/review-summary")
    public ApiResponse<LaboratoryReviewSummaryResponse> getReviewSummary(
            @PathVariable Long laboratoryId
    ) {
        LaboratoryReviewSummaryResponse response =
                laboratoryReviewService.getReviewSummary(
                        laboratoryId
                );

        return ApiResponse.success(response);
    }

    @GetMapping("/{laboratoryId}/reviews/me")
    public ApiResponse<LaboratoryReviewMeResponse> getMyReview(
            @PathVariable Long laboratoryId
    ) {
        Long userId = currentUserProvider.currentUserId()
                .orElseThrow(() ->
                        new IllegalStateException(
                                "AUTHENTICATION_REQUIRED"
                        )
                );

        LaboratoryReviewMeResponse response =
                laboratoryReviewService.getMyReview(
                        laboratoryId,
                        userId
                );

        return ApiResponse.success(response);
    }

    @PutMapping("/{laboratoryId}/reviews/{reviewId}")
    public ApiResponse<LaboratoryReviewUpdateResponse> updateReview(
            @PathVariable Long laboratoryId,
            @PathVariable Long reviewId,
            @Valid @RequestBody LaboratoryReviewUpdateRequest request
    ) {
        Long userId = currentUserProvider.currentUserId()
                .orElseThrow(() ->
                        new IllegalStateException(
                                "AUTHENTICATION_REQUIRED"
                        )
                );

        LaboratoryReviewUpdateResponse response =
                laboratoryReviewService.updateReview(
                        laboratoryId,
                        reviewId,
                        userId,
                        request
                );

        return ApiResponse.success(response);
    }

    @DeleteMapping("/{laboratoryId}/reviews/{reviewId}")
    public ApiResponse<LaboratoryReviewDeleteResponse> deleteReview(
            @PathVariable Long laboratoryId,
            @PathVariable Long reviewId
    ) {
        Long userId = currentUserProvider.currentUserId()
                .orElseThrow(() ->
                        new IllegalStateException(
                                "AUTHENTICATION_REQUIRED"
                        )
                );

        LaboratoryReviewDeleteResponse response =
                laboratoryReviewService.deleteReview(
                        laboratoryId,
                        reviewId,
                        userId
                );

        return ApiResponse.success(response);
    }
}
