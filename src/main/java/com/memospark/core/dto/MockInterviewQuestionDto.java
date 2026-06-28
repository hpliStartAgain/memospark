package com.memospark.core.dto;

import java.time.LocalDateTime;

public record MockInterviewQuestionDto(
        Long id,
        int questionOrder,
        String dimension,
        String question,
        String rubric,
        String userAnswer,
        Integer score,
        String feedback,
        LocalDateTime answeredAt
) {}
