package com.memospark.core.dto;

import java.util.List;

public record AnswerEvaluationDto(
        String grade,
        int quality,
        int score,
        String feedback,
        List<String> missingPoints,
        String suggestedAnswer,
        Integer recommendedReviewDays,
        String coachingTip,
        String learningMode
) {
    public AnswerEvaluationDto(
            String grade,
            int quality,
            int score,
            String feedback,
            List<String> missingPoints,
            String suggestedAnswer) {
        this(grade, quality, score, feedback, missingPoints, suggestedAnswer, null, "", "REVIEW");
    }
}
