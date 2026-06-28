package com.memospark.core.dto;

public record ReviewRequest(
        int quality,
        Long timeSpentMs,
        String userAnswer,
        String aiGrade,
        String aiFeedback,
        String aiSuggestedAnswer,
        Integer aiRecommendedReviewDays
) {
    public ReviewRequest(int quality, Long timeSpentMs) {
        this(quality, timeSpentMs, null, null, null, null, null);
    }

    public ReviewRequest(
            int quality,
            Long timeSpentMs,
            String userAnswer,
            String aiGrade,
            String aiFeedback,
            String aiSuggestedAnswer) {
        this(quality, timeSpentMs, userAnswer, aiGrade, aiFeedback, aiSuggestedAnswer, null);
    }
}
