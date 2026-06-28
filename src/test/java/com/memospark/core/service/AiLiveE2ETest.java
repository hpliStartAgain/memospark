package com.memospark.core.service;

import com.memospark.core.domain.User;
import com.memospark.core.domain.UserRole;
import com.memospark.core.dto.UpdateAiSettingsRequest;
import com.memospark.core.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@TestPropertySource(properties = {
        "ai.api.timeout-seconds=60",
        "ai.api.max-retries=1"
})
@EnabledIfEnvironmentVariable(named = "LIVE_AI_E2E", matches = "true")
class AiLiveE2ETest {

    @Autowired private UserRepository userRepository;
    @Autowired private AiSettingsService aiSettingsService;
    @Autowired private AiService aiService;

    @Test
    @Transactional
    void userConfiguredProvider_runsCoreAiFlows() {
        String apiKey = System.getenv("LIVE_AI_API_KEY");
        assertNotNull(apiKey, "LIVE_AI_API_KEY is required");
        assertFalse(apiKey.isBlank(), "LIVE_AI_API_KEY is required");

        User user = userRepository.save(new User(
                "live-ai-e2e-" + System.nanoTime(),
                "unused",
                UserRole.USER));

        var settings = aiSettingsService.update(user.getId(), new UpdateAiSettingsRequest(
                "SenseNova",
                "https://token.sensenova.cn/v1",
                "deepseek-v4-flash",
                apiKey,
                false));

        assertTrue(settings.apiKeyConfigured());
        assertNotEquals(apiKey, settings.apiKeyMasked());
        assertEquals("deepseek-v4-flash", settings.model());
        assertEquals("OK", aiService.testConnection(user.getId()).trim());

        Map<String, Object> jdAnalysis = aiService.analyzeJds(List.of(
                "Senior Java backend engineer. Spring Boot, MySQL, Redis, distributed systems, performance tuning."
        ), "zh", user.getId());
        assertTrue(jdAnalysis.containsKey("decks"));
        assertFalse(((List<?>) jdAnalysis.get("decks")).isEmpty());

        List<Map<String, String>> cards = aiService.generateCards(
                "Java concurrency: synchronized vs ReentrantLock", 2, "zh", user.getId());
        assertEquals(2, cards.size());
        assertTrue(cards.stream().allMatch(c -> c.get("front") != null && c.get("back") != null));

        List<Map<String, String>> questions = aiService.generateInterviewQuestions(
                "Target: Senior Java backend engineer. Skills: Spring Boot, Redis, system design.",
                "TECHNICAL",
                2,
                "zh",
                user.getId());
        assertEquals(2, questions.size());
        assertTrue(questions.stream().allMatch(q -> q.get("question") != null));

        Map<String, Object> evaluation = aiService.evaluateInterviewAnswer(
                questions.getFirst().get("question"),
                "我会先明确一致性和吞吐目标，再比较缓存、数据库和消息队列的取舍，并补充监控和降级方案。",
                questions.getFirst().getOrDefault("rubric", "正确性、深度、权衡和表达"),
                questions.getFirst().getOrDefault("dimension", "TECHNICAL"),
                "zh",
                user.getId());
        assertTrue(evaluation.containsKey("score"));
        assertNotNull(evaluation.get("feedback"));
    }
}
