package com.memospark.core.controller;

import com.memospark.core.config.CurrentUser;
import com.memospark.core.config.UserPrincipal;
import com.memospark.core.dto.*;
import com.memospark.core.service.AiService;
import com.memospark.core.service.JudgeOrchestrator;
import com.memospark.core.service.ProblemNoteService;
import com.memospark.core.service.ProblemService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/practice")
@RequiredArgsConstructor
public class PracticeController {

    private final ProblemService problemService;
    private final ProblemNoteService noteService;
    private final AiService aiService;
    private final JudgeOrchestrator judgeOrchestrator;

    @GetMapping("/problems")
    public List<ProblemSummaryDto> getAllProblems(@CurrentUser UserPrincipal principal) {
        return problemService.getAllProblemsSummary(principal != null ? principal.id() : null);
    }

    @GetMapping("/problems/{id}")
    public CodeProblemDetailDto getProblem(@PathVariable Long id, @CurrentUser UserPrincipal principal) {
        return problemService.getProblem(id, principal != null ? principal.id() : null);
    }

    @PostMapping("/problems/{id}/submit")
    public Map<String, Long> submitAsync(@PathVariable Long id, @RequestBody CodeSubmitRequest req,
                                         @CurrentUser UserPrincipal principal) throws Exception {
        Long submissionId = judgeOrchestrator.submitAsync(id, req, principal.id()).get();
        return Map.of("submissionId", submissionId);
    }

    @GetMapping(value = "/submissions/{submissionId}/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamSubmission(@PathVariable Long submissionId) {
        return judgeOrchestrator.subscribe(submissionId);
    }

    @GetMapping("/problems/{id}/submissions")
    public List<CodeSubmissionDto> getSubmissions(@PathVariable Long id,
                                                   @CurrentUser UserPrincipal principal) {
        return problemService.getSubmissions(id, principal.id());
    }

    // ── Admin CRUD ──

    @PostMapping("/problems")
    @ResponseStatus(HttpStatus.CREATED)
    public CodeProblemDto createProblem(@RequestBody CreateProblemRequest req) {
        return problemService.createProblem(req);
    }

    @PutMapping("/problems/{id}")
    public CodeProblemDto updateProblem(@PathVariable Long id, @RequestBody CreateProblemRequest req) {
        return problemService.updateProblem(id, req);
    }

    @DeleteMapping("/problems/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteProblem(@PathVariable Long id) {
        problemService.deleteProblem(id);
    }

    // ── Notebook Endpoints ──

    @GetMapping("/notebook")
    public List<ProblemNoteDto> getNotebook(
            @RequestParam(required = false) String type,
            @CurrentUser UserPrincipal principal) {
        if (type != null && !type.isBlank()) {
            return noteService.getNotesByType(principal.id(), type);
        }
        return noteService.getAllNotes(principal.id());
    }

    @GetMapping("/notebook/summary")
    public Map<String, Object> getNotebookSummary(@CurrentUser UserPrincipal principal) {
        return noteService.getSummary(principal.id());
    }

    @GetMapping("/notebook/due")
    public List<ProblemNoteDto> getDueNotes(@CurrentUser UserPrincipal principal) {
        return noteService.getDueNotes(principal.id());
    }

    @PostMapping("/notebook/{problemId}/toggle-star")
    public Map<String, Object> toggleStar(@PathVariable Long problemId,
                                           @CurrentUser UserPrincipal principal) {
        return noteService.toggleStar(principal.id(), problemId);
    }

    @PostMapping("/notebook/{problemId}/toggle-todo")
    public Map<String, Object> toggleTodo(@PathVariable Long problemId,
                                           @CurrentUser UserPrincipal principal) {
        return noteService.toggleTodo(principal.id(), problemId);
    }

    public record WrongNoteRequest(String note, String errorReason) {}

    @PostMapping("/notebook/{problemId}/wrong")
    public ProblemNoteDto saveWrongNote(@PathVariable Long problemId,
                                        @RequestBody WrongNoteRequest req,
                                        @CurrentUser UserPrincipal principal) {
        return noteService.saveWrongNote(principal.id(), problemId, req.note(), req.errorReason());
    }

    @DeleteMapping("/notebook/{problemId}/wrong")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeWrongNote(@PathVariable Long problemId, @CurrentUser UserPrincipal principal) {
        noteService.removeWrongNote(principal.id(), problemId);
    }

    public record RetryRequest(int quality) {}
    public record ConvertToCardRequest(Long deckId) {}

    @PostMapping("/notebook/{problemId}/retry")
    public ProblemNoteDto recordRetry(@PathVariable Long problemId,
                                      @RequestBody RetryRequest req,
                                      @CurrentUser UserPrincipal principal) {
        return noteService.recordRetry(principal.id(), problemId, req.quality());
    }

    @PostMapping("/notebook/{problemId}/to-card")
    @ResponseStatus(HttpStatus.CREATED)
    public ReviewCardDto convertToCard(@PathVariable Long problemId,
                                       @RequestBody ConvertToCardRequest req,
                                       @CurrentUser UserPrincipal principal) {
        if (req.deckId() == null) {
            throw new IllegalArgumentException("deckId is required");
        }
        return noteService.convertToCard(principal.id(), problemId, req.deckId());
    }

    @PostMapping("/notebook/ai-analysis")
    public WeaknessAnalysisDto aiAnalysis(@CurrentUser UserPrincipal principal) {
        WeaknessAnalysisDto data = noteService.getWeaknessData(principal.id());
        if (data.totalWrong() == 0) {
            return data;
        }

        StringBuilder summary = new StringBuilder();
        summary.append("Error reasons: ");
        data.errorReasonCounts().forEach((k, v) -> summary.append(k).append("=").append(v).append(", "));
        summary.append("\nWeak categories: ");
        data.categoryCounts().forEach((k, v) -> summary.append(k).append("=").append(v).append(", "));
        summary.append("\nTotal wrong problems: ").append(data.totalWrong());

        String aiText = aiService.analyzeWeakness(summary.toString(), principal.id());
        return new WeaknessAnalysisDto(data.errorReasonCounts(), data.categoryCounts(), data.totalWrong(), aiText);
    }
}
