package com.memospark.core.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.memospark.core.domain.BookmarkType;
import com.memospark.core.domain.CodeProblem;
import com.memospark.core.domain.CodeSubmission;
import com.memospark.core.domain.ProblemNote;
import com.memospark.core.domain.User;
import com.memospark.core.dto.*;
import com.memospark.core.repository.CodeProblemRepository;
import com.memospark.core.repository.CodeSubmissionRepository;
import com.memospark.core.repository.ProblemNoteRepository;
import com.memospark.core.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProblemService {

    private final CodeProblemRepository problemRepository;
    private final CodeSubmissionRepository submissionRepository;
    private final ProblemNoteRepository noteRepository;
    private final UserRepository userRepository;
    private final JudgeService judgeService;
    private final ProblemNoteService problemNoteService;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public List<ProblemSummaryDto> getAllProblemsSummary(Long userId) {
        // All batch-fetched — zero N+1 per problem
        Map<Long, ProblemNote> noteMap = new HashMap<>();
        Map<Long, Integer> failMap = new HashMap<>();
        Set<Long> acceptedIds = new HashSet<>();
        Map<Long, Integer> attemptMap = new HashMap<>();

        if (userId != null) {
            noteRepository.findByUserIdOrderByUpdatedAtDesc(userId)
                    .forEach(n -> noteMap.put(n.getProblem().getId(), n));
            for (Object[] row : submissionRepository.countFailsByUser(userId)) {
                failMap.put((Long) row[0], ((Long) row[1]).intValue());
            }
            for (Object[] row : submissionRepository.countAttemptsByUser(userId)) {
                attemptMap.put((Long) row[0], ((Long) row[1]).intValue());
            }
            submissionRepository.findAcceptedProblemIdsByUser(userId)
                    .forEach(acceptedIds::add);
        }

        return problemRepository.findAllByOrderByProblemNumberAsc()
                .stream().map(p -> {
                    ProblemNote note = noteMap.get(p.getId());
                    String bm = note != null && note.getBookmarkType() != null ? note.getBookmarkType().name() : null;
                    boolean starred = note != null && note.isStarred();
                    int failCount = failMap.getOrDefault(p.getId(), 0);
                    int attemptCount = attemptMap.getOrDefault(p.getId(), 0);
                    boolean accepted = acceptedIds.contains(p.getId());
                    return new ProblemSummaryDto(
                            p.getId(), p.getProblemNumber(), p.getTitle(), p.getDifficulty(),
                            p.getCategory(), p.getTags(), p.getHint(),
                            accepted, bm, starred, failCount, attemptCount);
                }).toList();
    }

    @Transactional(readOnly = true)
    public List<CodeProblemDto> getAllProblems(Long userId) {
        Map<Long, ProblemNote> noteMap = new HashMap<>();
        Map<Long, Integer> failMap = new HashMap<>();
        Set<Long> acceptedIds = new HashSet<>();
        Map<Long, Integer> attemptMap = new HashMap<>();

        if (userId != null) {
            noteRepository.findByUserIdOrderByUpdatedAtDesc(userId)
                    .forEach(n -> noteMap.put(n.getProblem().getId(), n));
            for (Object[] row : submissionRepository.countFailsByUser(userId)) {
                failMap.put((Long) row[0], ((Long) row[1]).intValue());
            }
            for (Object[] row : submissionRepository.countAttemptsByUser(userId)) {
                attemptMap.put((Long) row[0], ((Long) row[1]).intValue());
            }
            submissionRepository.findAcceptedProblemIdsByUser(userId)
                    .forEach(acceptedIds::add);
        }
        final Map<Long, ProblemNote> notes = noteMap;
        final Map<Long, Integer> fails = failMap;
        final Set<Long> accepted = acceptedIds;
        final Map<Long, Integer> attempts = attemptMap;
        return problemRepository.findAllByOrderByProblemNumberAsc()
                .stream().map(p -> {
                    ProblemNote note = notes.get(p.getId());
                    String bm = note != null && note.getBookmarkType() != null ? note.getBookmarkType().name() : null;
                    boolean starred = note != null && note.isStarred();
                    int failCount = fails.getOrDefault(p.getId(), 0);
                    int attemptCount = attempts.getOrDefault(p.getId(), 0);
                    return toDtoWithCounts(p, accepted.contains(p.getId()), bm, starred, failCount, attemptCount);
                }).toList();
    }

    @Transactional(readOnly = true)
    public CodeProblemDto getProblem(Long id, Long userId) {
        CodeProblem p = getProblemOrThrow(id);
        String bookmark = null;
        boolean starred = false;
        int failCount = 0;
        if (userId != null) {
            var noteOpt = noteRepository.findByUserIdAndProblemId(userId, id);
            if (noteOpt.isPresent()) {
                ProblemNote note = noteOpt.get();
                bookmark = note.getBookmarkType() != null ? note.getBookmarkType().name() : null;
                starred = note.isStarred();
            }
            failCount = submissionRepository.countByProblemIdAndUserId(id, userId);
        }
        boolean accepted = userId != null && submissionRepository.existsByProblemIdAndUserIdAndStatus(id, userId, "ACCEPTED");
        int attemptCount = userId != null ? submissionRepository.countByProblemIdAndUserId(id, userId) : 0;
        return toDtoWithCounts(p, accepted, bookmark, starred, failCount, attemptCount);
    }

    @Transactional
    public CodeSubmitResultDto submit(Long problemId, CodeSubmitRequest req, Long userId) {
        CodeProblem problem = getProblemOrThrow(problemId);
        User user = userRepository.findById(userId).orElse(null);

        String driverCode = "java".equals(req.language())
                ? problem.getJavaDriverCode()
                : problem.getPythonDriverCode();

        String fullCode = driverCode.replace("{{USER_CODE}}", req.code());

        List<Map<String, String>> testCases = parseTestCases(problem.getTestCasesJson());

        List<CodeSubmitResultDto.TestCaseResult> results = new ArrayList<>();
        int passed = 0;
        String overallStatus = "ACCEPTED";

        for (int i = 0; i < testCases.size(); i++) {
            Map<String, String> tc = testCases.get(i);
            String input = tc.get("input");
            String expected = tc.get("expectedOutput").trim();

            JudgeService.JudgeResult judgeResult = judgeService.execute(fullCode, req.language(), input);

            String actual = judgeResult.stdout().trim();
            boolean pass;

            if (judgeResult.statusId() == 6) {
                overallStatus = "COMPILE_ERROR";
                actual = judgeResult.compileOutput();
                pass = false;
            } else if (judgeResult.statusId() == 5) {
                overallStatus = "TIME_LIMIT";
                actual = "Time Limit Exceeded";
                pass = false;
            } else if (judgeResult.statusId() != 3) {
                overallStatus = "RUNTIME_ERROR";
                actual = judgeResult.stderr().isEmpty() ? "Runtime Error" : judgeResult.stderr();
                pass = false;
            } else {
                pass = expected.equals(actual);
                if (!pass && "ACCEPTED".equals(overallStatus)) {
                    overallStatus = "WRONG_ANSWER";
                }
            }

            if (pass) passed++;
            results.add(new CodeSubmitResultDto.TestCaseResult(i + 1, pass, input, expected, actual));

            // Stop on compilation/runtime error — fill remaining as not executed
            if (judgeResult.statusId() != 3) {
                for (int j = i + 1; j < testCases.size(); j++) {
                    Map<String, String> remaining = testCases.get(j);
                    results.add(new CodeSubmitResultDto.TestCaseResult(
                            j + 1, false, remaining.get("input"),
                            remaining.get("expectedOutput"), "Not executed"));
                }
                break;
            }
        }

        // Persist submission
        CodeSubmission submission = new CodeSubmission(
                problem, user, req.language(), req.code(), overallStatus, passed, testCases.size());
        submissionRepository.save(submission);

        // Auto-update retry schedule if user has a WRONG note and got ACCEPTED.
        // Delegates to the shared SM-2 engine via ProblemNoteService (quality 5 = perfect recall).
        if ("ACCEPTED".equals(overallStatus) && userId != null) {
            noteRepository.findByUserIdAndProblemId(userId, problemId).ifPresent(note -> {
                if (note.getBookmarkType() == BookmarkType.WRONG) {
                    problemNoteService.recordRetry(userId, problemId, 5);
                }
            });
        }

        return new CodeSubmitResultDto(submission.getId(), overallStatus, passed, testCases.size(), results);
    }

    @Transactional(readOnly = true)
    public List<CodeSubmissionDto> getSubmissions(Long problemId, Long userId) {
        return submissionRepository.findByProblemIdAndUserIdOrderBySubmittedAtDesc(problemId, userId)
                .stream()
                .map(s -> new CodeSubmissionDto(
                        s.getId(), s.getLanguage(), s.getStatus(),
                        s.getPassedCases(), s.getTotalCases(), s.getSubmittedAt()))
                .toList();
    }

    private CodeProblem getProblemOrThrow(Long id) {
        return problemRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Problem not found: " + id));
    }

    @Transactional
    public CodeProblemDto createProblem(CreateProblemRequest req) {
        CodeProblem p = new CodeProblem(
                req.problemNumber(), req.title(), req.difficulty(),
                req.description(), req.javaTemplate(), req.pythonTemplate(),
                req.javaDriverCode(), req.pythonDriverCode(),
                req.testCasesJson(), req.tags());
        p.setHint(req.hint());
        p.setCategory(req.category());
        p = problemRepository.save(p);
        return toDtoWithCounts(p, false, null, false, 0, 0);
    }

    @Transactional
    public CodeProblemDto updateProblem(Long id, CreateProblemRequest req) {
        CodeProblem p = getProblemOrThrow(id);
        if (req.problemNumber() != null) p.setProblemNumber(req.problemNumber());
        if (req.title() != null) p.setTitle(req.title());
        if (req.difficulty() != null) p.setDifficulty(req.difficulty());
        if (req.description() != null) p.setDescription(req.description());
        if (req.hint() != null) p.setHint(req.hint());
        if (req.javaTemplate() != null) p.setJavaTemplate(req.javaTemplate());
        if (req.pythonTemplate() != null) p.setPythonTemplate(req.pythonTemplate());
        if (req.javaDriverCode() != null) p.setJavaDriverCode(req.javaDriverCode());
        if (req.pythonDriverCode() != null) p.setPythonDriverCode(req.pythonDriverCode());
        if (req.testCasesJson() != null) p.setTestCasesJson(req.testCasesJson());
        if (req.tags() != null) p.setTags(req.tags());
        if (req.category() != null) p.setCategory(req.category());
        p = problemRepository.save(p);
        return toDtoWithCounts(p, false, null, false, 0, 0);
    }

    @Transactional
    public void deleteProblem(Long id) {
        // Detach notes so users keep their review notes
        noteRepository.clearProblemReference(id);
        submissionRepository.deleteByProblemId(id);
        problemRepository.deleteById(id);
    }

    private CodeProblemDto toDtoWithCounts(CodeProblem p, boolean accepted, String bookmarkType,
                                            boolean starred, int failCount, int attemptCount) {
        return new CodeProblemDto(
                p.getId(), p.getProblemNumber(), p.getTitle(), p.getDifficulty(),
                p.getDescription(), p.getHint(),
                p.getJavaTemplate(), p.getPythonTemplate(),
                p.getJavaDriverCode(), p.getPythonDriverCode(),
                p.getTestCasesJson(), p.getTags(), p.getCategory(),
                accepted, bookmarkType, starred, failCount, attemptCount);
    }

    private List<Map<String, String>> parseTestCases(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse test cases", e);
        }
    }
}
