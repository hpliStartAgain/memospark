package com.memospark.core.dto;

import java.time.LocalDate;

public record StudyPlanItemDto(
        Long id,
        LocalDate date,
        String type,
        Long deckId,
        String deckName,
        String stage,
        String title,
        String objective,
        int targetCount,
        long completedCount,
        boolean completed
) {}

