package com.memospark.core.dto;

import jakarta.validation.constraints.NotBlank;

public record QuickAddCardRequest(
        @NotBlank String username,
        @NotBlank String deckName,
        @NotBlank String front,
        @NotBlank String back,
        String tags
) {}
