package com.memospark.core.dto;

import com.fasterxml.jackson.databind.JsonNode;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record StudyPlanDto(
        Long id,
        Long targetId,
        String targetTitle,
        LocalDate startDate,
        LocalDate targetDate,
        int weeklyHours,
        String summary,
        String strategy,
        JsonNode roadmap,
        List<StudyPlanWeekDto> weeks,
        LocalDateTime generatedAt
) {}

