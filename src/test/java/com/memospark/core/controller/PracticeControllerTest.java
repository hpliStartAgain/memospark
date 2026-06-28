package com.memospark.core.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.memospark.core.config.JwtService;
import com.memospark.core.domain.User;
import com.memospark.core.domain.UserRole;
import com.memospark.core.dto.CodeProblemDetailDto;
import com.memospark.core.dto.CodeSubmissionDto;
import com.memospark.core.repository.UserRepository;
import com.memospark.core.service.JudgeOrchestrator;
import com.memospark.core.service.ProblemNoteService;
import com.memospark.core.service.ProblemService;
import com.memospark.core.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Slice test for PracticeController — verifies that the problem detail endpoint
 * returns CodeProblemDetailDto (without testCasesJson).
 */
@WebMvcTest(controllers = PracticeController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = com.memospark.core.config.SecurityConfig.class))
@AutoConfigureMockMvc(addFilters = false)
@WithMockUser(username = "u")
class PracticeControllerTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper om;

    @MockitoBean
    ProblemService problemService;

    @MockitoBean
    ProblemNoteService noteService;

    @MockitoBean
    JudgeOrchestrator judgeOrchestrator;

    @MockitoBean
    com.memospark.core.service.AiService aiService;

    @MockitoBean
    UserRepository userRepository;

    @MockitoBean
    JwtService jwtService;

    @MockitoBean
    UserService userService;

    @BeforeEach
    void setUpUser() {
        User user = new User("u", "p", UserRole.USER);
        user.setId(1L);
        when(userRepository.findByUsername("u")).thenReturn(Optional.of(user));
    }

    @Test
    void getProblem_returnsDetailDtoWithoutTestCases() throws Exception {
        CodeProblemDetailDto dto = new CodeProblemDetailDto(
                1L, 1, "Two Sum", "Easy",
                "desc", "hint",
                "java template", "python template",
                "tags", "category",
                false, null, false, 0, 0
        );
        when(problemService.getProblem(eq(1L), any())).thenReturn(dto);

        mvc.perform(get("/api/practice/problems/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Two Sum"))
                .andExpect(jsonPath("$.testCasesJson").doesNotExist())
                .andExpect(jsonPath("$.javaDriverCode").doesNotExist())
                .andExpect(jsonPath("$.pythonDriverCode").doesNotExist());
    }

    @Test
    void getSubmissions_returnsList() throws Exception {
        when(problemService.getSubmissions(eq(1L), any()))
                .thenReturn(List.of(new CodeSubmissionDto(
                        1L, "java", "ACCEPTED", 3, 3, LocalDateTime.now())));

        mvc.perform(get("/api/practice/problems/1/submissions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("ACCEPTED"));
    }
}
