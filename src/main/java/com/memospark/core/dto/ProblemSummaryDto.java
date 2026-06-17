package com.memospark.core.dto;

/**
 * Lightweight DTO returned by the problem list endpoint.
 * Does NOT include driver code, test cases, or templates — those
 * are only returned by the individual problem detail endpoint.
 */
public record ProblemSummaryDto(
        Long id,
        Integer problemNumber,
        String title,
        String difficulty,
        String category,
        String tags,
        String hint,
        boolean accepted,
        String bookmarkType,
        boolean starred,
        int failCount,
        int attemptCount
) {}
