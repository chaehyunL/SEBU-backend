package com.sebu.backend.auth.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record UpdateGradeRequest(
    @NotNull
    @Min(1)
    @Max(4)
    Integer grade
) {
}
