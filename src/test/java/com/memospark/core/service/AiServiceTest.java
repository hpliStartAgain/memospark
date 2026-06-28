package com.memospark.core.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class AiServiceTest {

    private AiService aiService;

    @BeforeEach
    void setUp() {
        aiService = new AiService(new ObjectMapper(), mock(AiSettingsService.class));
        ReflectionTestUtils.setField(aiService, "apiKey", "test-key");
        ReflectionTestUtils.setField(aiService, "apiUrl", "http://localhost/v1/chat");
        ReflectionTestUtils.setField(aiService, "model", "test-model");
        ReflectionTestUtils.setField(aiService, "timeoutSeconds", 5);
        ReflectionTestUtils.setField(aiService, "maxRetries", 1);
    }

    @Test
    void generateCards_emptyResponse_throwsRuntimeException() {
        // The chat() method will fail because there's no real HTTP endpoint,
        // but we can test the retry logic by verifying the exception type
        assertThrows(RuntimeException.class,
                () -> aiService.generateCards("test", 5, "en"));
    }

    @Test
    void gradeAnswer_aiUnavailable_returnsFallback() {
        // When AI is unavailable, gradeAnswer catches the exception and returns fallback
        Map<String, Object> result = aiService.gradeAnswer("Q", "A", "student answer");
        // The chat() will throw RuntimeException after retries, but gradeAnswer catches it
        // and returns a fallback map
        assertEquals("C", result.get("grade"));
        assertEquals(3, result.get("quality"));
        assertNotNull(result.get("feedback"));
    }

    @Test
    void generateHint_aiUnavailable_throws() {
        assertThrows(RuntimeException.class,
                () -> aiService.generateHint("problem", null, 1));
    }

    @Test
    void analyzeTLE_aiUnavailable_throws() {
        assertThrows(RuntimeException.class,
                () -> aiService.analyzeTLE("problem", "code", "java"));
    }

    @Test
    void analyzeJds_aiUnavailable_throws() {
        assertThrows(RuntimeException.class,
                () -> aiService.analyzeJds(List.of("JD text"), "en"));
    }

    @Test
    void analyzeWeakness_aiUnavailable_throws() {
        assertThrows(RuntimeException.class,
                () -> aiService.analyzeWeakness("summary text"));
    }
}
