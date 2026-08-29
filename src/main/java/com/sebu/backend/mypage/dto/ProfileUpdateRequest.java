package com.sebu.backend.mypage.dto;

import com.sebu.backend.user.domain.GpaBand;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ProfileUpdateRequest (

    String nickname,

    @NotNull
    @Min(1)
    @Max(4)
    Short grade,

    GpaBand gpaBand,

    @NotNull
    @Size(max = 500)
    String introduction
){}
