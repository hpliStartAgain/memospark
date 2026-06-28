package com.memospark.core.dto;

public record TargetSkillDto(
        Long id,
        String name,
        String category,
        String description,
        int weight,
        int selfLevel,
        Long deckId,
        long cardCount,
        String deckLinkSource,
        String matchedDeckName,
        Double deckMatchScore
) {}
