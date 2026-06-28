package com.memospark.core.service;

import com.memospark.core.domain.*;
import com.memospark.core.dto.*;
import com.memospark.core.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
public class MockInterviewService {

    private static final int DEFAULT_QUESTION_COUNT = 5;
    private static final int MAX_QUESTION_COUNT = 8;

    private final TargetRepository targetRepository;
    private final JobJdRepository jobJdRepository;
    private final TargetSkillRepository targetSkillRepository;
    private final MockInterviewRepository interviewRepository;
    private final MockInterviewQuestionRepository questionRepository;
    private final AiService aiService;

    @Transactional(readOnly = true)
    public List<MockInterviewDto> list(Long targetId, Long userId, boolean admin) {
        getOwnedTarget(targetId, userId, admin);
        return interviewRepository.findByTargetIdOrderByStartedAtDesc(targetId).stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public MockInterviewDto get(Long targetId, Long interviewId, Long userId, boolean admin) {
        getOwnedTarget(targetId, userId, admin);
        return toDto(getInterview(targetId, interviewId));
    }

    @Transactional
    public MockInterviewDto start(Long targetId, Long userId, boolean admin, StartMockInterviewRequest req) {
        Target target = getOwnedTarget(targetId, userId, admin);
        MockInterviewType type = parseType(req != null ? req.type() : null);
        int count = clamp(req != null && req.count() != null ? req.count() : DEFAULT_QUESTION_COUNT, 1, MAX_QUESTION_COUNT);
        String language = req != null && req.language() != null && !req.language().isBlank() ? req.language() : "zh";

        List<Map<String, String>> generated = aiService.generateInterviewQuestions(
                buildTargetContext(target), type.name(), count, language, userId);
        if (generated.isEmpty()) {
            throw new RuntimeException("AI returned no mock interview questions");
        }

        MockInterview interview = new MockInterview(target, target.getUser(), type, generated.size());
        int order = 1;
        for (Map<String, String> row : generated) {
            String question = clean(row.get("question"));
            if (question == null) continue;
            String dimension = clean(row.getOrDefault("dimension", type.name()));
            String rubric = clean(row.getOrDefault("rubric", "Score correctness, depth, structure, and communication."));
            interview.addQuestion(new MockInterviewQuestion(order++, normalizeDimension(dimension), question, rubric));
        }
        interview.setQuestionCount(interview.getQuestions().size());
        if (interview.getQuestionCount() == 0) {
            throw new RuntimeException("AI returned no usable mock interview questions");
        }

        return toDto(interviewRepository.save(interview));
    }

    @Transactional
    public MockInterviewDto answer(Long targetId, Long interviewId, Long questionId,
                                   Long userId, boolean admin, AnswerMockInterviewRequest req) {
        getOwnedTarget(targetId, userId, admin);
        MockInterview interview = getInterview(targetId, interviewId);
        MockInterviewQuestion question = questionRepository.findByIdAndInterviewId(questionId, interviewId)
                .orElseThrow(() -> new NoSuchElementException("Mock interview question not found"));

        String answer = req != null ? clean(req.answer()) : null;
        if (answer == null) {
            throw new IllegalArgumentException("Answer is required");
        }

        Map<String, Object> evaluation = aiService.evaluateInterviewAnswer(
                question.getQuestion(),
                answer,
                question.getRubric(),
                question.getDimension(),
                "zh",
                userId);

        question.setUserAnswer(answer);
        question.setScore(clamp(parseScore(evaluation.get("score")), 0, 100));
        question.setFeedback(composeFeedback(evaluation));
        question.setAnsweredAt(LocalDateTime.now());
        questionRepository.save(question);

        refreshAggregate(interview);
        return toDto(interviewRepository.save(interview));
    }

    @Transactional
    public MockInterviewDto finish(Long targetId, Long interviewId, Long userId, boolean admin) {
        getOwnedTarget(targetId, userId, admin);
        MockInterview interview = getInterview(targetId, interviewId);
        refreshAggregate(interview);
        interview.setStatus(MockInterviewStatus.FINISHED);
        if (interview.getFinishedAt() == null) {
            interview.setFinishedAt(LocalDateTime.now());
        }
        return toDto(interviewRepository.save(interview));
    }

    private Target getOwnedTarget(Long targetId, Long userId, boolean admin) {
        Target target = targetRepository.findById(targetId)
                .orElseThrow(() -> new NoSuchElementException("Target not found"));
        if (!admin && !target.getUser().getId().equals(userId)) {
            throw new SecurityException("Not your target");
        }
        return target;
    }

    private MockInterview getInterview(Long targetId, Long interviewId) {
        return interviewRepository.findByIdAndTargetId(interviewId, targetId)
                .orElseThrow(() -> new NoSuchElementException("Mock interview not found"));
    }

    private String buildTargetContext(Target target) {
        StringBuilder sb = new StringBuilder();
        sb.append("Target: ").append(target.getTitle()).append('\n');
        if (target.getCompany() != null) sb.append("Company: ").append(target.getCompany()).append('\n');
        if (target.getNotes() != null) sb.append("Notes: ").append(target.getNotes()).append('\n');

        sb.append("\nSkills:\n");
        for (TargetSkill skill : targetSkillRepository.findByTargetIdOrderByWeightDescIdAsc(target.getId())) {
            sb.append("- ").append(skill.getName())
                    .append(" (weight ").append(skill.getWeight())
                    .append(", self level ").append(skill.getSelfLevel()).append(")");
            if (skill.getDescription() != null) sb.append(": ").append(skill.getDescription());
            sb.append('\n');
        }

        sb.append("\nJob descriptions:\n");
        int index = 1;
        for (JobJd jd : jobJdRepository.findByTargetIdOrderByCreatedAtDesc(target.getId())) {
            sb.append("--- JD #").append(index++).append(" ---\n")
                    .append(truncate(jd.getContent(), 3500))
                    .append("\n\n");
        }
        return truncate(sb.toString(), 12000);
    }

    private void refreshAggregate(MockInterview interview) {
        List<MockInterviewQuestion> questions = questionRepository.findByInterviewIdOrderByQuestionOrderAsc(interview.getId());
        long answered = questions.stream().filter(q -> q.getScore() != null).count();
        if (answered > 0) {
            double average = questions.stream()
                    .filter(q -> q.getScore() != null)
                    .mapToInt(MockInterviewQuestion::getScore)
                    .average()
                    .orElse(0.0);
            interview.setAverageScore(Math.round(average * 10.0) / 10.0);
        }

        if (answered == questions.size() && !questions.isEmpty()) {
            interview.setStatus(MockInterviewStatus.FINISHED);
            if (interview.getFinishedAt() == null) {
                interview.setFinishedAt(LocalDateTime.now());
            }
        }
        interview.setSummaryFeedback(summaryFor(interview, (int) answered, questions.size()));
    }

    private String summaryFor(MockInterview interview, int answered, int total) {
        if (answered == 0) {
            return "尚未作答。";
        }
        String score = interview.getAverageScore() != null ? String.valueOf(interview.getAverageScore()) : "0";
        if (answered < total) {
            return "已回答 " + answered + "/" + total + " 题，当前平均分 " + score + "。";
        }
        return "模拟面试完成，平均分 " + score + "。优先复盘低分题的反馈，并把薄弱点转成后续复习材料。";
    }

    private MockInterviewDto toDto(MockInterview interview) {
        List<MockInterviewQuestionDto> questions = questionRepository
                .findByInterviewIdOrderByQuestionOrderAsc(interview.getId())
                .stream()
                .map(this::toQuestionDto)
                .toList();
        int answered = (int) questions.stream().filter(q -> q.score() != null).count();
        return new MockInterviewDto(
                interview.getId(),
                interview.getTarget().getId(),
                interview.getType().name(),
                interview.getStatus().name(),
                interview.getQuestionCount(),
                answered,
                interview.getAverageScore(),
                interview.getSummaryFeedback(),
                interview.getStartedAt(),
                interview.getFinishedAt(),
                questions
        );
    }

    private MockInterviewQuestionDto toQuestionDto(MockInterviewQuestion q) {
        return new MockInterviewQuestionDto(
                q.getId(),
                q.getQuestionOrder(),
                q.getDimension(),
                q.getQuestion(),
                q.getRubric(),
                q.getUserAnswer(),
                q.getScore(),
                q.getFeedback(),
                q.getAnsweredAt()
        );
    }

    private MockInterviewType parseType(String value) {
        if (value == null || value.isBlank()) return MockInterviewType.MIXED;
        try {
            return MockInterviewType.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return MockInterviewType.MIXED;
        }
    }

    private String normalizeDimension(String value) {
        if (value == null || value.isBlank()) return "TECHNICAL";
        String upper = value.trim().toUpperCase().replace('-', '_').replace(' ', '_');
        if (upper.contains("STAR") || upper.contains("BEHAVIOR")) return "STAR";
        if (upper.contains("SYSTEM")) return "SYSTEM_DESIGN";
        if (upper.contains("DESIGN")) return "SYSTEM_DESIGN";
        return "TECHNICAL";
    }

    private int parseScore(Object value) {
        if (value instanceof Number n) return n.intValue();
        if (value instanceof String s) {
            try {
                return (int) Math.round(Double.parseDouble(s.trim()));
            } catch (NumberFormatException ignored) {
                return 60;
            }
        }
        return 60;
    }

    private String composeFeedback(Map<String, Object> evaluation) {
        StringBuilder sb = new StringBuilder();
        Object feedback = evaluation.get("feedback");
        if (feedback != null) sb.append(feedback);
        appendList(sb, "优势", evaluation.get("strengths"));
        appendList(sb, "改进", evaluation.get("improvements"));
        return sb.isEmpty() ? "已评分。" : sb.toString();
    }

    private void appendList(StringBuilder sb, String title, Object value) {
        if (!(value instanceof List<?> list) || list.isEmpty()) return;
        if (!sb.isEmpty()) sb.append('\n');
        sb.append(title).append("：");
        for (Object item : list) {
            if (item != null) sb.append("\n- ").append(item);
        }
    }

    private int clamp(int v, int min, int max) {
        return Math.max(min, Math.min(max, v));
    }

    private String clean(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String truncate(String value, int max) {
        if (value == null || value.length() <= max) return value;
        return value.substring(0, max) + "\n...[truncated]";
    }
}
