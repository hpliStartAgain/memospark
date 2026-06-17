package com.memospark.core.dto;

import jakarta.validation.constraints.NotBlank;

public record QuickAddDeckRequest(
        @NotBlank String username,
        String name,
        String description,
        Integer dailyReviewLimit,
        Integer dailyNewCardLimit
) {}
