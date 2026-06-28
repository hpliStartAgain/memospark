package com.memospark.core.dto;

import java.util.List;

public record AnswerExplanationRequest(
        String userAnswer,
        String message,
        List<AnswerChatMessageDto> history
) {}
