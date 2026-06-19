package com.memospark.core.dto;

public record CodeProblemDetailDto(
        Long id,
        int problemNumber,
        String title,
        String difficulty,
        String description,
        String hint,
        String javaTemplate,
        String pythonTemplate,
        String tags,
        String category,
        boolean accepted,
        String bookmarkType,
        boolean starred,
        int failCount,
        int attemptCount
) {}
