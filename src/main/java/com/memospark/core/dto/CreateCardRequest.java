package com.memospark.core.dto;

public record CreateCardRequest(
        String front,
        String back,
        String tags,
        String contentDifficulty,
        String learningStage,
        Integer stageOrder
) {
    public CreateCardRequest(String front, String back, String tags) {
        this(front, back, tags, null, null, null);
    }
}
