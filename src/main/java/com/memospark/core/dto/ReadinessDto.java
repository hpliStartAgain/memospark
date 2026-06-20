package com.memospark.core.dto;

public record ReadinessDto(
        int overall,
        int skillCoverage,
        int cardHealth,
        int wrongClear,
        long dueCards,
        long dueNotes,
        long weakSkills,
        Long daysUntilInterview
) {}
