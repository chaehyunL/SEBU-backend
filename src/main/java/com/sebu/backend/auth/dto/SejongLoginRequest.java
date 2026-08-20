package com.sebu.backend.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record SejongLoginRequest(
    @NotBlank String studentId,
    @NotBlank String password
) {
    @Override
    public String toString() {
        return "SejongLoginRequest[studentId=REDACTED, password=REDACTED]";
    }
}
