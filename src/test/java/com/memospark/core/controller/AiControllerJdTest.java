package com.memospark.core.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.memospark.core.config.JwtService;
import com.memospark.core.repository.UserRepository;
import com.memospark.core.service.AiService;
import com.memospark.core.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Slice test for the JD-driven endpoints in {@link AiController}.
 *
 * Excludes the application's SecurityConfig to keep the slice minimal;
 * disables filters so {@link org.springframework.security.core.annotation.AuthenticationPrincipal}
 * is unbound (null) which is acceptable for these endpoints.
 */
@WebMvcTest(controllers = AiController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = com.memospark.core.config.SecurityConfig.class))
@AutoConfigureMockMvc(addFilters = false)
class AiControllerJdTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper om;

    @MockitoBean
    AiService aiService;

    @MockitoBean
    UserRepository userRepository;

    @MockitoBean
    JwtService jwtService;

    @MockitoBean
    UserService userService;

    @Test
    void analyzeJds_happyPath_returnsDecksFromService() throws Exception {
        when(aiService.analyzeJds(any(), eq("zh"))).thenReturn(Map.of(
                "decks", List.of(
                        Map.of("name", "Redis", "description", "缓存与数据结构",
                                "topics", List.of("RDB/AOF", "缓存击穿"),
                                "suggestedCardCount", 12)
                )
        ));

        String body = om.writeValueAsString(Map.of(
                "jds", List.of("Senior backend at Foo: Java, Spring, Redis, MySQL"),
                "language", "zh"
        ));

        mvc.perform(post("/api/ai/jd/analyze")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.decks[0].name").value("Redis"))
                .andExpect(jsonPath("$.decks[0].topics[0]").value("RDB/AOF"));
    }

    @Test
    void analyzeJds_emptyJds_returns400() throws Exception {
        String body = om.writeValueAsString(Map.of("jds", List.of(), "language", "en"));
        mvc.perform(post("/api/ai/jd/analyze")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void analyzeJds_nullJds_returns400() throws Exception {
        // Body without jds field -> record's jds() == null
        String body = "{\"language\":\"en\"}";
        mvc.perform(post("/api/ai/jd/analyze")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void generateJdCards_happyPath_capsCountAt30() throws Exception {
        when(aiService.generateCardsForTopic(eq("Redis"), eq("RDB/AOF"), eq(30), eq("zh")))
                .thenReturn(List.of(
                        Map.of("front", "Q1", "back", "A1", "tags", "redis"),
                        Map.of("front", "Q2", "back", "A2", "tags", "redis")
                ));

        String body = om.writeValueAsString(Map.of(
                "deckName", "Redis",
                "topic", "RDB/AOF",
                "count", 999,
                "language", "zh"
        ));

        mvc.perform(post("/api/ai/jd/generate-cards")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].front").value("Q1"))
                .andExpect(jsonPath("$[1].back").value("A2"));

        verify(aiService).generateCardsForTopic("Redis", "RDB/AOF", 30, "zh");
    }

    @Test
    void generateJdCards_defaultsCountTo10WhenNonPositive() throws Exception {
        when(aiService.generateCardsForTopic(anyString(), anyString(), anyInt(), anyString()))
                .thenReturn(List.of());

        String body = om.writeValueAsString(Map.of(
                "deckName", "Redis",
                "topic", "RDB/AOF",
                "count", 0,
                "language", "en"
        ));

        mvc.perform(post("/api/ai/jd/generate-cards")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());

        verify(aiService).generateCardsForTopic("Redis", "RDB/AOF", 10, "en");
    }

    @Test
    void generateJdCards_blankTopic_returns400() throws Exception {
        String body = om.writeValueAsString(Map.of(
                "deckName", "Redis",
                "topic", "  ",
                "count", 5,
                "language", "en"
        ));
        mvc.perform(post("/api/ai/jd/generate-cards")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }
}
