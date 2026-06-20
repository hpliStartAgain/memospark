package com.memospark.core.dto;

public record AnalyzeTargetRequest(
        String language,
        boolean replace
) {}
