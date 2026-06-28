package com.memospark.core.dto;

public record ReadinessDto(
        int overall,
        int skillCoverage,
        int cardHealth,
        int wrongClear,
        int mockPerformance,
        long dueCards,
        long dueNotes,
        long weakSkills,
        Long daysUntilInterview
) {}
