package com.sebu.backend.laboratoryreview.dto;

import java.util.List;
import java.util.Map;

public record LaboratoryReviewSummaryResponse(
        LaboratoryInfo laboratory,
        Double averageRating,
        long reviewCount,
        List<RatingDistribution> ratingDistribution,
        EvaluationDistributions evaluationDistributions
) {

    public record LaboratoryInfo(
            Long id,
            String name,
            ProfessorInfo professor,
            CollegeInfo college,
            DepartmentInfo department
    ) {
    }

    public record ProfessorInfo(
            Long id,
            String name
    ) {
    }

    public record CollegeInfo(
            Long id,
            String name
    ) {
    }

    public record DepartmentInfo(
            Long id,
            String name
    ) {
    }

    public record RatingDistribution(
            int rating,
            long count,
            double percentage
    ) {
    }

    public record EvaluationDistribution(
            String value,
            long count,
            double percentage
    ) {
    }

    public record EvaluationDistributions(
            List<EvaluationDistribution> researchIntensity,
            List<EvaluationDistribution> compensation,
            List<EvaluationDistribution> paperOpportunity,
            List<EvaluationDistribution> atmosphere
    ) {
    }
}
