package com.memospark.core.service;

import com.memospark.core.domain.*;
import com.memospark.core.dto.AnswerMockInterviewRequest;
import com.memospark.core.dto.MockInterviewDto;
import com.memospark.core.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MockInterviewServiceTest {

    @Mock private TargetRepository targetRepository;
    @Mock private JobJdRepository jobJdRepository;
    @Mock private TargetSkillRepository targetSkillRepository;
    @Mock private MockInterviewRepository interviewRepository;
    @Mock private MockInterviewQuestionRepository questionRepository;
    @Mock private AiService aiService;

    @InjectMocks
    private MockInterviewService service;

    private Target target;
    private MockInterview interview;
    private MockInterviewQuestion q1;
    private MockInterviewQuestion q2;

    @BeforeEach
    void setUp() {
        User user = new User("u", "p", UserRole.USER);
        user.setId(1L);
        target = new Target(user, "Backend Engineer", "ACME");
        target.setId(10L);

        interview = new MockInterview(target, user, MockInterviewType.TECHNICAL, 2);
        interview.setId(100L);

        q1 = new MockInterviewQuestion(1, "TECHNICAL", "Explain Redis persistence.", "correctness; tradeoffs");
        q1.setId(1001L);
        q1.setInterview(interview);
        q2 = new MockInterviewQuestion(2, "SYSTEM_DESIGN", "Design a feed.", "requirements; scale");
        q2.setId(1002L);
        q2.setInterview(interview);
        q2.setUserAnswer("Use fanout and cache.");
        q2.setScore(75);
    }

    @Test
    void answer_lastQuestion_scoresAndFinishesInterview() {
        when(targetRepository.findById(10L)).thenReturn(Optional.of(target));
        when(interviewRepository.findByIdAndTargetId(100L, 10L)).thenReturn(Optional.of(interview));
        when(questionRepository.findByIdAndInterviewId(1001L, 100L)).thenReturn(Optional.of(q1));
        when(aiService.evaluateInterviewAnswer(
                anyString(), anyString(), anyString(), anyString(), anyString(), anyLong()))
                .thenReturn(Map.of(
                        "score", 85,
                        "feedback", "Good tradeoff explanation.",
                        "strengths", List.of("Clear structure"),
                        "improvements", List.of("Add failure modes")));
        when(questionRepository.findByInterviewIdOrderByQuestionOrderAsc(100L)).thenReturn(List.of(q1, q2));
        when(interviewRepository.save(any(MockInterview.class))).thenAnswer(inv -> inv.getArgument(0));

        MockInterviewDto dto = service.answer(
                10L, 100L, 1001L, 1L, false,
                new AnswerMockInterviewRequest("RDB snapshots plus AOF, with latency and recovery tradeoffs."));

        assertEquals(MockInterviewStatus.FINISHED.name(), dto.status());
        assertEquals(2, dto.answeredCount());
        assertEquals(80.0, dto.averageScore());
        assertNotNull(dto.finishedAt());
        assertTrue(q1.getFeedback().contains("Good tradeoff"));
        verify(questionRepository).save(q1);
        verify(interviewRepository).save(interview);
    }
}
