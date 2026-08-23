package com.sebu.backend.mypage.dto;

import com.sebu.backend.user.domain.GpaBand;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ProfileUpdateRequest (

    @NotBlank
    @Size(max = 30)
    String name,

    @NotNull
    @Min(1)
    @Max(4)
    Short grade,

    @NotNull
    String majorId,

    GpaBand gpaBand,

    @NotNull
    @Size(max = 500)
    String introduction
){}
