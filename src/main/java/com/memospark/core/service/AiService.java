package com.memospark.core.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@Slf4j
@RequiredArgsConstructor
public class AiService {

    @Value("${ai.api.key}")
    private String apiKey;

    @Value("${ai.api.url}")
    private String apiUrl;

    @Value("${ai.api.model:qwen-turbo}")
    private String model;

    @Value("${ai.api.timeout-seconds:30}")
    private int timeoutSeconds;

    @Value("${ai.api.max-retries:2}")
    private int maxRetries;

    private final ObjectMapper objectMapper;
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    /**
     * Grade a user's answer against the question and reference answer.
     * Returns JSON: {"grade":"A/B/C/D/E","quality":5/4/3/2/0,"feedback":"..."}
     */
    public Map<String, Object> gradeAnswer(String question, String referenceAnswer, String userAnswer) {
        String prompt = """
                You are a strict but fair teacher grading a student's flashcard answer.

                Question: %s

                Reference Answer: %s

                Student's Answer: %s

                Grade the student's answer from A to E:
                - A: Excellent, covers all key points accurately
                - B: Good, mostly correct with minor omissions
                - C: Acceptable, knows the general idea but missing important details
                - D: Poor, significant errors or very incomplete
                - E: Wrong or no meaningful answer

                Reply in the same language as the question. Return ONLY JSON, no markdown:
                {"grade":"A","quality":5,"feedback":"brief explanation of the grade"}

                Mapping: A→quality 5, B→quality 4, C→quality 3, D→quality 2, E→quality 0
                """.formatted(question, referenceAnswer, userAnswer);

        String response;
        try {
            response = chat(prompt);
        } catch (RuntimeException e) {
            log.warn("AI grade unavailable, returning fallback grade", e);
            return Map.of("grade", "C", "quality", 3,
                    "feedback", "AI grading is temporarily unavailable.");
        }
        try {
            return extractJsonObject(response, new TypeReference<>() {});
        } catch (Exception e) {
            log.error("Failed to parse AI grade response: {}", response, e);
            return Map.of("grade", "C", "quality", 3, "feedback", response);
        }
    }

    /**
     * Get a progressive hint for a coding problem.
     * level: 1=general direction, 2=approach detail, 3=pseudocode
     */
    public String generateHint(String problemDescription, String userCode, int level) {
        String levelDesc = switch (level) {
            case 1 -> "Give a brief general direction/approach (1-2 sentences). Do NOT give code or detailed steps.";
            case 2 -> "Give a more detailed approach with key steps and data structures to use. Do NOT give actual code.";
            case 3 -> "Give pseudocode or a very detailed step-by-step algorithm. Still don't give the complete solution code.";
            default -> "Give a brief hint.";
        };

        String prompt = """
                You are a coding tutor. A student is working on this problem:

                %s

                %s

                %s

                Reply in the same language as the problem description (Chinese or English). Keep it concise.
                """.formatted(
                problemDescription,
                (userCode != null && !userCode.isBlank()) ? "Their current code:\n" + userCode : "",
                levelDesc
        );

        return chat(prompt);
    }

    /**
     * Analyze why user's code got TLE or Runtime Error.
     * Returns optimization suggestions without giving complete solution code.
     */
    public String analyzeTLE(String problemDescription, String userCode, String language) {
        String prompt = """
                You are a coding tutor. A student submitted code that got Time Limit Exceeded (TLE) or Runtime Error.

                Problem:
                %s

                Student's code (%s):
                %s

                Analyze the code and provide:
                1. The time complexity of their current approach
                2. Why it's too slow or causes an error
                3. What optimization approach they should consider (without giving complete code)
                4. The target time complexity they should aim for

                Reply in the same language as the problem description. Be concise and educational.
                Do NOT provide the complete solution code, only guide the student.
                """.formatted(problemDescription, language, userCode);

        return chat(prompt);
    }

    /**
     * Generate flashcards for a given topic.
     */
    public List<Map<String, String>> generateCards(String topic, int count, String language) {
        String lang = "zh".equals(language) ? "Chinese" : "English";

        String prompt = """
                Generate exactly %d flashcards about the topic: "%s"

                Each card should have a question (front) and a concise answer (back).
                Reply in %s.

                Return ONLY a JSON array, no markdown, no explanation:
                [{"front":"question here","back":"answer here","tags":"tag1,tag2"}]
                """.formatted(count, topic, lang);

        String response = chat(prompt);

        try {
            return extractJsonArray(response, new TypeReference<>() {});
        } catch (Exception e) {
            log.error("Failed to parse AI card response: {}", response, e);
            throw new RuntimeException("AI returned invalid card format");
        }
    }

    /**
     * Analyze multiple Job Descriptions and propose common tech-area decks.
     * Each deck has a name, description, and a list of sub-topics (which will be
     * used as prompts for batched card generation).
     *
     * Returns: {"decks":[{"name":"...","description":"...","topics":["..."],"suggestedCardCount":10}]}
     */
    public Map<String, Object> analyzeJds(List<String> jds, String language) {
        String lang = "zh".equals(language) ? "Chinese" : "English";
        StringBuilder joined = new StringBuilder();
        int idx = 1;
        for (String jd : jds) {
            if (jd == null || jd.isBlank()) continue;
            joined.append("--- JD #").append(idx++).append(" ---\n").append(jd.trim()).append("\n\n");
        }

        String prompt = """
                You are a senior technical recruiter and interview coach. A candidate pasted %d target job descriptions (JDs) below.
                Extract the COMMON required technical areas across these JDs (weighted by frequency), then propose a set of study decks that,
                together, best cover what the candidate must master to pass interviews for these roles.

                For each proposed deck, give:
                - name: short topic name (a concrete tech area, e.g. "Redis 原理与实战" or "System Design Fundamentals")
                - description: 1 sentence explaining why it is needed across these JDs
                - topics: 3-8 concrete sub-topics (each will be used to generate flashcards separately; keep each sub-topic specific enough that ~10 Q&A cards can be produced)
                - suggestedCardCount: integer 8-20, total recommended cards for this deck

                Guidelines:
                - Prefer 5-10 decks total; merge closely related areas.
                - Skip soft-skill / HR-only items.
                - If JDs disagree, favour areas appearing in most JDs.

                Reply in %s. Return ONLY JSON, no markdown:
                {"decks":[{"name":"...","description":"...","topics":["...","..."],"suggestedCardCount":12}]}

                JDs:
                %s
                """.formatted(idx - 1, lang, joined.toString());

        String response = chat(prompt);
        try {
            return extractJsonObject(response, new TypeReference<>() {});
        } catch (Exception e) {
            log.error("Failed to parse AI JD analysis response: {}", response, e);
            throw new RuntimeException("AI returned invalid JD analysis format");
        }
    }

    /**
     * Generate flashcards for a specific sub-topic inside a deck.
     * Thin wrapper around generateCards with deck context for better targeting.
     */
    public List<Map<String, String>> generateCardsForTopic(String deckName, String topic, int count, String language) {
        String composed = (deckName == null || deckName.isBlank())
                ? topic
                : deckName + " - " + topic;
        return generateCards(composed, count, language);
    }

    /**
     * Analyze user's weakness patterns based on wrong problem data.
     */
    public String analyzeWeakness(String summary) {
        String prompt = """
                You are a coding tutor analyzing a student's weakness patterns.

                Here is a summary of their wrong problems:
                %s

                Based on this data, provide:
                1. The student's main weak areas
                2. Why these areas might be challenging
                3. Specific study recommendations (which topics to review, what type of practice to do)
                4. A suggested study order

                Reply in the same language as the summary. Be concise and actionable.
                """.formatted(summary);

        return chat(prompt);
    }

    /**
     * Streaming chat: calls AI API with stream=true and invokes onChunk for each content delta.
     * Returns the full accumulated text.
     */
    public String chatStream(String userMessage, Consumer<String> onChunk) {
        try {
            String body = objectMapper.writeValueAsString(Map.of(
                    "model", model,
                    "messages", List.of(Map.of("role", "user", "content", userMessage)),
                    "stream", true
            ));

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(apiUrl))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .timeout(Duration.ofSeconds(timeoutSeconds))
                    .build();

            StringBuilder full = new StringBuilder();
            httpClient.send(request, HttpResponse.BodyHandlers.ofLines())
                    .body()
                    .forEach(line -> {
                        if (line.startsWith("data: ")) {
                            String data = line.substring(6).trim();
                            if ("[DONE]".equals(data)) return;
                            try {
                                JsonNode node = objectMapper.readTree(data);
                                String delta = node.path("choices").path(0)
                                        .path("delta").path("content").asText("");
                                if (!delta.isEmpty()) {
                                    full.append(delta);
                                    onChunk.accept(delta);
                                }
                            } catch (Exception e) {
                                log.debug("Failed to parse SSE chunk: {}", data);
                            }
                        }
                    });
            return full.toString();
        } catch (Exception e) {
            if (e instanceof InterruptedException) Thread.currentThread().interrupt();
            log.error("AI streaming call failed", e);
            throw new RuntimeException("AI streaming unavailable: " + e.getMessage());
        }
    }

    /**
     * Generate a hint with streaming support.
     * @param onChunk callback invoked for each text chunk as it arrives
     * @return the full hint text
     */
    public String generateHintStream(String problemDescription, String userCode, int level,
                                     Consumer<String> onChunk) {
        String levelDesc = switch (level) {
            case 1 -> "general direction (don't reveal the algorithm)";
            case 2 -> "approach detail (name the algorithm/approach)";
            case 3 -> "pseudocode-level hint";
            default -> "general direction";
        };

        String prompt = """
                You are a helpful coding interview tutor.
                Give a progressive hint at level: %s.

                Problem: %s

                Student's current code:
                ```
                %s
                ```

                Reply in the same language as the problem description. Be concise.
                """.formatted(levelDesc, problemDescription,
                        userCode != null ? userCode : "(no code yet)");

        return chatStream(prompt, onChunk);
    }

    private String chat(String userMessage) {
        RuntimeException last = null;
        for (int attempt = 0; attempt <= maxRetries; attempt++) {
            if (attempt > 0) {
                long backoffMs = (long) Math.min(4000, 500 * Math.pow(2, attempt - 1));
                log.warn("AI call retry {}/{} after {}ms", attempt, maxRetries, backoffMs);
                sleep(backoffMs);
            }
            try {
                return chatOnce(userMessage);
            } catch (RetryableAiException e) {
                last = e;
            }
        }
        throw new RuntimeException("AI service unavailable: "
                + (last != null ? last.getMessage() : "unknown error"));
    }

    private String chatOnce(String userMessage) {
        try {
            String body = objectMapper.writeValueAsString(Map.of(
                    "model", model,
                    "messages", List.of(Map.of("role", "user", "content", userMessage))
            ));

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(apiUrl))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .timeout(Duration.ofSeconds(timeoutSeconds))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            int sc = response.statusCode();
            if (sc != 200) {
                log.error("AI API error {}: {}", sc, response.body());
                // 429 / 5xx are transient and worth retrying; 4xx (except 429) are not.
                if (sc == 429 || sc >= 500) {
                    throw new RetryableAiException("AI service error: " + sc);
                }
                throw new RuntimeException("AI service error: " + sc);
            }

            JsonNode root = objectMapper.readTree(response.body());
            return root.path("choices").path(0)
                    .path("message").path("content").asText("");
        } catch (java.io.IOException | InterruptedException e) {
            if (e instanceof InterruptedException) Thread.currentThread().interrupt();
            log.error("AI API call failed (transient)", e);
            throw new RetryableAiException("AI service unavailable: " + e.getMessage());
        }
    }

    private void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static class RetryableAiException extends RuntimeException {
        RetryableAiException(String message) {
            super(message);
        }
    }

    // ── JSON extraction helpers ───────────────────────────────────────────

    private static final Pattern JSON_OBJECT_PATTERN = Pattern.compile("\\{.*}", Pattern.DOTALL);
    private static final Pattern JSON_ARRAY_PATTERN = Pattern.compile("\\[.*\\]", Pattern.DOTALL);

    private <T> T extractJsonObject(String response, TypeReference<T> typeRef) throws Exception {
        String cleaned = stripMarkdownFences(response);
        Matcher m = JSON_OBJECT_PATTERN.matcher(cleaned);
        String json = m.find() ? m.group() : cleaned;
        return objectMapper.readValue(json, typeRef);
    }

    private <T> List<Map<String, String>> extractJsonArray(String response, TypeReference<List<Map<String, String>>> typeRef) throws Exception {
        String cleaned = stripMarkdownFences(response);
        Matcher m = JSON_ARRAY_PATTERN.matcher(cleaned);
        String json = m.find() ? m.group() : cleaned;
        return objectMapper.readValue(json, typeRef);
    }

    private String stripMarkdownFences(String text) {
        String trimmed = text.trim();
        if (trimmed.startsWith("```")) {
            int firstNewline = trimmed.indexOf('\n');
            if (firstNewline > 0) {
                trimmed = trimmed.substring(firstNewline + 1);
            }
            int lastFence = trimmed.lastIndexOf("```");
            if (lastFence >= 0) {
                trimmed = trimmed.substring(0, lastFence);
            }
        }
        return trimmed.trim();
    }
}
