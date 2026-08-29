package com.sebu.backend.laboratoryreview.dto;

import com.sebu.backend.laboratoryreview.domain.LaboratoryReview;

import java.time.LocalDateTime;
import java.util.List;

public record LaboratoryReviewListResponse(
        List<ReviewItem> reviews,
        int page,
        int size,
        long totalElements,
        boolean hasNext
) {

    public record ReviewItem(
            Long id,
            int overallRating,
            String researchIntensity,
            String compensation,
            String paperOpportunity,
            String atmosphere,
            String content,
            boolean mine,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
        public static ReviewItem from(
                LaboratoryReview review,
                Long currentUserId
        ) {
            return new ReviewItem(
                    review.getId(),
                    review.getOverallRating(),
                    review.getResearchIntensity().name(),
                    review.getCompensation().name(),
                    review.getPaperOpportunity().name(),
                    review.getAtmosphere().name(),
                    review.getContent(),
                    currentUserId != null
                            && review.isWrittenBy(currentUserId),
                    review.getCreatedAt(),
                    review.getUpdatedAt()
            );
        }
    }
}
