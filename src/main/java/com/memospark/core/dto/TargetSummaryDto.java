package com.memospark.core.dto;

import java.time.LocalDate;

public record TargetSummaryDto(
        Long id,
        String title,
        String company,
        String status,
        LocalDate interviewDate,
        Long daysUntilInterview,
        long jdCount,
        long skillCount,
        int readiness
) {}
