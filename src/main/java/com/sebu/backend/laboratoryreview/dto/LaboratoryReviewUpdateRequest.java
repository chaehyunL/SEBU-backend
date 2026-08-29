package com.sebu.backend.laboratoryreview.dto;

import com.sebu.backend.laboratoryreview.domain.Atmosphere;
import com.sebu.backend.laboratoryreview.domain.Compensation;
import com.sebu.backend.laboratoryreview.domain.PaperOpportunity;
import com.sebu.backend.laboratoryreview.domain.ParticipationTerm;
import com.sebu.backend.laboratoryreview.domain.ResearchIntensity;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record LaboratoryReviewUpdateRequest(

        @NotNull
        @Min(1)
        @Max(5)
        Integer overallRating,

        @NotNull
        ResearchIntensity researchIntensity,

        @NotNull
        Compensation compensation,

        @NotNull
        PaperOpportunity paperOpportunity,

        @NotNull
        Atmosphere atmosphere,

        @NotBlank
        @Size(min = 20, max = 2000)
        String content,

        @NotNull
        @Min(2000)
        Integer participationYear,

        @NotNull
        ParticipationTerm participationTerm
) {
}
