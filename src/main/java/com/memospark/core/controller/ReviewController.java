package com.memospark.core.controller;

import com.memospark.core.config.CurrentUser;
import com.memospark.core.config.UserPrincipal;
import com.memospark.core.dto.AnswerEvaluationDto;
import com.memospark.core.dto.AnswerEvaluationRequest;
import com.memospark.core.dto.AnswerExplanationDto;
import com.memospark.core.dto.AnswerExplanationRequest;
import com.memospark.core.dto.ReviewCardDto;
import com.memospark.core.dto.ReviewRequest;
import com.memospark.core.service.ReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/review")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    @GetMapping("/today")
    public List<ReviewCardDto> getTodaysDueCards(@CurrentUser UserPrincipal principal) {
        return reviewService.getTodaysDueCards(principal.id());
    }

    @GetMapping("/deck/{deckId}")
    public List<ReviewCardDto> getDueCardsByDeck(@PathVariable Long deckId,
                                                  @RequestParam(required = false) List<String> tags,
                                                  @CurrentUser UserPrincipal principal) {
        return reviewService.getDueCardsByDeck(deckId, tags, principal.id(), principal.admin());
    }

    @PostMapping("/{cardId}")
    public ReviewCardDto submitReview(@PathVariable Long cardId,
                                      @RequestBody ReviewRequest req,
                                      @CurrentUser UserPrincipal principal) {
        return reviewService.submitReview(cardId, req, principal.id());
    }

    @PostMapping("/{cardId}/evaluate-answer")
    public AnswerEvaluationDto evaluateAnswer(@PathVariable Long cardId,
                                              @RequestBody(required = false) AnswerEvaluationRequest req,
                                              @CurrentUser UserPrincipal principal) {
        return reviewService.evaluateAnswer(cardId, req, principal.id());
    }

    @PostMapping("/{cardId}/explain-answer")
    public AnswerExplanationDto explainAnswer(@PathVariable Long cardId,
                                              @RequestBody AnswerExplanationRequest req,
                                              @CurrentUser UserPrincipal principal) {
        return reviewService.explainAnswer(cardId, req, principal.id());
    }

    @GetMapping("/hard")
    public List<ReviewCardDto> getHardCards(@CurrentUser UserPrincipal principal) {
        return reviewService.getHardCards(principal.id());
    }

    @PostMapping("/{cardId}/undo")
    public ReviewCardDto undoLastReview(@PathVariable Long cardId, @CurrentUser UserPrincipal principal) {
        return reviewService.undoLastReview(cardId, principal.id());
    }
}
