package com.songhighlights.interfaces.web.dto;

import jakarta.validation.constraints.NotBlank;

public record ChatRequest(
        @NotBlank String userId,
        @NotBlank String message
) {
}
