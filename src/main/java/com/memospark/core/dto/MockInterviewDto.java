package com.memospark.core.dto;

import java.time.LocalDateTime;
import java.util.List;

public record MockInterviewDto(
        Long id,
        Long targetId,
        String type,
        String status,
        int questionCount,
        int answeredCount,
        Double averageScore,
        String summaryFeedback,
        LocalDateTime startedAt,
        LocalDateTime finishedAt,
        List<MockInterviewQuestionDto> questions
) {}
