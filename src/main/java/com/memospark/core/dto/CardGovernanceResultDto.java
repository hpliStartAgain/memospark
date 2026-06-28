package com.memospark.core.dto;

import java.util.List;

public record CardGovernanceResultDto(
        int updatedCards,
        String summary,
        List<ReviewCardDto> cards
) {}

