package com.sebu.backend.laboratoryreview.dto;

import com.sebu.backend.laboratoryreview.domain.LaboratoryReview;

import java.time.LocalDateTime;

public record LaboratoryReviewMeResponse(
        Review review
) {

    public record Review(
            Long id,
            int overallRating,
            String researchIntensity,
            String compensation,
            String paperOpportunity,
            String atmosphere,
            String content,
            int participationYear,
            String participationTerm,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {

        public static Review from(LaboratoryReview review) {
            return new Review(
                    review.getId(),
                    review.getOverallRating(),
                    review.getResearchIntensity().name(),
                    review.getCompensation().name(),
                    review.getPaperOpportunity().name(),
                    review.getAtmosphere().name(),
                    review.getContent(),
                    review.getParticipationYear(),
                    review.getParticipationTerm().name(),
                    review.getCreatedAt(),
                    review.getUpdatedAt()
            );
        }
    }

    public static LaboratoryReviewMeResponse from(
            LaboratoryReview review
    ) {
        return new LaboratoryReviewMeResponse(
                Review.from(review)
        );
    }
}
