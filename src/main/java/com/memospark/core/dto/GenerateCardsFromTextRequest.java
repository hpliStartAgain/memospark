package com.memospark.core.dto;

public record GenerateCardsFromTextRequest(
        String text,
        Integer count,
        String language
) {}
