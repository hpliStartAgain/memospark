package com.memospark.core.dto;

public record UpdateAiSettingsRequest(
        String provider,
        String baseUrl,
        String model,
        String apiKey,
        Boolean clearApiKey
) {}
