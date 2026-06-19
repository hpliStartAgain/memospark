package com.memospark.core.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.memospark.core.domain.BookmarkType;
import com.memospark.core.domain.CodeProblem;
import com.memospark.core.domain.CodeSubmission;
import com.memospark.core.domain.ProblemNote;
import com.memospark.core.domain.User;
import com.memospark.core.dto.CodeSubmitRequest;
import com.memospark.core.dto.CodeSubmitResultDto;
import com.memospark.core.repository.CodeProblemRepository;
import com.memospark.core.repository.CodeSubmissionRepository;
import com.memospark.core.repository.ProblemNoteRepository;
import com.memospark.core.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import jakarta.annotation.PreDestroy;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Orchestrates async code submission execution.
 * <p>
 * Flow:
 * <ol>
 *   <li>Controller calls {@link #submitAsync} → immediately saves a PENDING submission, returns its ID.</li>
 *   <li>Frontend polls {@link #subscribe} SSE stream for the submission ID.</li>
 *   <li>Background task runs all test cases (parallel per-case), emits progress events, finalises submission.</li>
 * </ol>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class JudgeOrchestrator {

    private final CodeProblemRepository problemRepository;
    private final CodeSubmissionRepository submissionRepository;
    private final UserRepository userRepository;
    private final JudgeBackend judgeBackend;
    private final ProblemNoteRepository problemNoteRepository;
    private final ProblemNoteService problemNoteService;
    private final ObjectMapper objectMapper;

    /** In-flight SSE emitters keyed by submissionId. */
    private final ConcurrentHashMap<Long, SseEmitter> emitters = new ConcurrentHashMap<>();

    /** Bounded executor for parallel test case execution. */
    private final ExecutorService executor = Executors.newFixedThreadPool(
            Math.max(2, Runtime.getRuntime().availableProcessors()));

    @PreDestroy
    public void shutdown() {
        log.info("Shutting down JudgeOrchestrator executor...");
        executor.shutdown();
        // Close any lingering SSE emitters
        emitters.values().forEach(emitter -> {
            try { emitter.complete(); } catch (Exception ignored) {}
        });
        emitters.clear();
    }

    // ── Public API ─────────────────────────────────────────────────────────

    /**
     * Saves a PENDING submission record and schedules async execution.
     * @return the submission ID for the client to subscribe to.
     */
    @Async
    public CompletableFuture<Long> submitAsync(Long problemId, CodeSubmitRequest req, Long userId) {
        CodeProblem problem = problemRepository.findById(problemId)
                .orElseThrow(() -> new NoSuchElementException("Problem not found: " + problemId));
        User user = userId != null ? userRepository.findById(userId).orElse(null) : null;

        CodeSubmission pending = new CodeSubmission();
        pending.setProblem(problem);
        pending.setUser(user);
        pending.setLanguage(req.language());
        pending.setCode(req.code());
        pending.setStatus("PENDING");
        pending.setPassedCases(0);
        pending.setTotalCases(0);
        pending.setSubmittedAt(LocalDateTime.now());
        pending = submissionRepository.save(pending);

        final Long submissionId = pending.getId();
        final CodeSubmission savedPending = pending;

        executor.submit(() -> runJudge(savedPending, problem, req));

        return CompletableFuture.completedFuture(submissionId);
    }

    /**
     * Returns an SSE emitter that pushes events until the submission is complete.
     * If the submission is already finished, sends the final result immediately.
     */
    public SseEmitter subscribe(Long submissionId) {
        SseEmitter emitter = new SseEmitter(120_000L); // 2-min timeout

        Optional<CodeSubmission> opt = submissionRepository.findById(submissionId);
        if (opt.isPresent() && !opt.get().getStatus().equals("PENDING")) {
            sendFinalEvent(emitter, opt.get());
            return emitter;
        }

        emitters.put(submissionId, emitter);
        emitter.onCompletion(() -> emitters.remove(submissionId));
        emitter.onTimeout(() -> emitters.remove(submissionId));
        emitter.onError(e -> emitters.remove(submissionId));
        return emitter;
    }

    // ── Internal judge execution ───────────────────────────────────────────

    private void runJudge(CodeSubmission submission, CodeProblem problem, CodeSubmitRequest req) {
        long submissionId = submission.getId();
        try {
            String driverCode = "java".equals(req.language())
                    ? problem.getJavaDriverCode()
                    : problem.getPythonDriverCode();
            String fullCode = driverCode.replace("{{USER_CODE}}", req.code());
            List<Map<String, String>> testCases = parseTestCases(problem.getTestCasesJson());
            int total = testCases.size();

            // Run all test cases in parallel and aggregate in original order.
            AtomicInteger done = new AtomicInteger(0);
            List<CompletableFuture<CaseOutcome>> futures = new ArrayList<>();
            for (int i = 0; i < total; i++) {
                final int idx = i;
                final Map<String, String> tc = testCases.get(i);
                futures.add(CompletableFuture.supplyAsync(() -> {
                    CaseOutcome o = runCase(idx, tc, fullCode, req.language());
                    int completed = done.incrementAndGet();
                    emit(submissionId, "progress",
                            Map.of("case", completed, "total", total, "status", "RUNNING"));
                    return o;
                }, executor));
            }
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

            List<CodeSubmitResultDto.TestCaseResult> results = new ArrayList<>();
            int passed = 0;
            List<String> caseStatuses = new ArrayList<>();
            for (CompletableFuture<CaseOutcome> f : futures) {
                CaseOutcome o = f.join();
                results.add(o.result());
                caseStatuses.add(o.status());
                if (o.result().passed()) passed++;
            }
            String overallStatus = aggregateStatus(caseStatuses);

            // Persist final result
            submission.setStatus(overallStatus);
            submission.setPassedCases(passed);
            submission.setTotalCases(total);
            submissionRepository.save(submission);

            // Auto-update retry schedule if user has a WRONG note and got ACCEPTED
            if ("ACCEPTED".equals(overallStatus) && submission.getUser() != null) {
                problemNoteRepository.findByUserIdAndProblemId(
                        submission.getUser().getId(), problem.getId()).ifPresent(note -> {
                    if (note.getBookmarkType() == BookmarkType.WRONG) {
                        problemNoteService.recordRetry(submission.getUser().getId(), problem.getId(), 5);
                    }
                });
            }

            // Notify subscriber
            SseEmitter emitter = emitters.get(submissionId);
            if (emitter != null) {
                CodeSubmitResultDto result = new CodeSubmitResultDto(
                        submissionId, overallStatus, passed, total, results);
                sendEvent(emitter, "result", result);
                emitter.complete();
                emitters.remove(submissionId);
            }

        } catch (Exception e) {
            log.error("Judge execution failed for submission {}", submissionId, e);
            submission.setStatus("SYSTEM_ERROR");
            submissionRepository.save(submission);
            SseEmitter emitter = emitters.get(submissionId);
            if (emitter != null) {
                emit(submissionId, "error", Map.of("message", "Execution failed: " + e.getMessage()));
                emitter.completeWithError(e);
                emitters.remove(submissionId);
            }
        }
    }

    /** Per-case result paired with its normalized status for aggregation. */
    private record CaseOutcome(CodeSubmitResultDto.TestCaseResult result, String status) {}

    /** Executes a single test case and maps the judge result to a CaseOutcome. */
    private CaseOutcome runCase(int index, Map<String, String> tc,
                                String fullCode, String language) {
        String input = tc.get("input");
        String expected = tc.get("expectedOutput").trim();
        JudgeBackend.JudgeResult jr = judgeBackend.execute(fullCode, language, input);
        String actual = jr.stdout().trim();
        boolean pass;
        String status;
        if (jr.statusId() == 6) {
            actual = jr.compileOutput();
            pass = false;
            status = "COMPILE_ERROR";
        } else if (jr.statusId() == 5) {
            actual = "Time Limit Exceeded";
            pass = false;
            status = "TIME_LIMIT";
        } else if (jr.statusId() != 3) {
            actual = jr.stderr().isEmpty() ? "Runtime Error" : jr.stderr();
            pass = false;
            status = "RUNTIME_ERROR";
        } else {
            pass = expected.equals(actual);
            status = pass ? "ACCEPTED" : "WRONG_ANSWER";
        }
        return new CaseOutcome(
                new CodeSubmitResultDto.TestCaseResult(index + 1, pass, input, expected, actual),
                status);
    }

    /** Derives the overall submission status from per-case statuses (priority ordered). */
    private String aggregateStatus(List<String> caseStatuses) {
        if (caseStatuses.contains("COMPILE_ERROR")) return "COMPILE_ERROR";
        if (caseStatuses.contains("TIME_LIMIT")) return "TIME_LIMIT";
        if (caseStatuses.contains("RUNTIME_ERROR")) return "RUNTIME_ERROR";
        if (caseStatuses.contains("WRONG_ANSWER")) return "WRONG_ANSWER";
        return "ACCEPTED";
    }

    private void emit(Long submissionId, String eventName, Object data) {
        SseEmitter emitter = emitters.get(submissionId);
        if (emitter != null) {
            sendEvent(emitter, eventName, data);
        }
    }

    private void sendEvent(SseEmitter emitter, String eventName, Object data) {
        try {
            String json = objectMapper.writeValueAsString(data);
            emitter.send(SseEmitter.event().name(eventName).data(json));
        } catch (Exception e) {
            log.warn("SSE send failed for event '{}': {}", eventName, e.getMessage());
        }
    }

    private void sendFinalEvent(SseEmitter emitter, CodeSubmission sub) {
        try {
            CodeSubmitResultDto result = new CodeSubmitResultDto(
                    sub.getId(), sub.getStatus(), sub.getPassedCases(), sub.getTotalCases(), List.of());
            sendEvent(emitter, "result", result);
            emitter.complete();
        } catch (Exception e) {
            log.warn("Failed to send final SSE event: {}", e.getMessage());
        }
    }

    private List<Map<String, String>> parseTestCases(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse test cases", e);
        }
    }
}
