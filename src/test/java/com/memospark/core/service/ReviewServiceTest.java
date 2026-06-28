package com.memospark.core.service;

import com.memospark.core.domain.*;
import com.memospark.core.dto.AnswerEvaluationDto;
import com.memospark.core.dto.AnswerEvaluationRequest;
import com.memospark.core.dto.ReviewCardDto;
import com.memospark.core.dto.ReviewRequest;
import com.memospark.core.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReviewServiceTest {

    @Mock private CardService cardService;
    @Mock private CardProgressRepository cardProgressRepository;
    @Mock private ReviewLogRepository reviewLogRepository;
    @Mock private DeckRepository deckRepository;
    @Mock private DeckService deckService;
    @Mock private SpacedRepetitionService srsService;
    @Mock private AiService aiService;

    @InjectMocks
    private ReviewService reviewService;

    private Card card;
    private CardProgress progress;
    private Deck deck;

    @BeforeEach
    void setUp() {
        deck = new Deck("Test", "", DeckType.CUSTOM, new User("u", "p", UserRole.USER));
        deck.setId(1L);
        card = new Card(deck, "front", "back", "tag1");
        card.setId(10L);
        progress = new CardProgress(card);
        progress.setRepetitions(2);
        progress.setEaseFactor(2.5);
        progress.setStability(4.0);
        progress.setDifficulty(5.0);
        progress.setInterval(6);
        progress.setNextReviewDate(LocalDate.now().plusDays(6));
        progress.setLastReviewDate(LocalDate.now().minusDays(6));
        progress.setFirstLearnedDate(LocalDate.now().minusDays(20));
    }

    @Test
    void submitReview_quality5_appliesReviewAndSavesLog() {
        when(cardService.getCardOrThrow(10L)).thenReturn(card);
        when(cardProgressRepository.findByCardId(10L)).thenReturn(Optional.of(progress));

        ReviewCardDto result = reviewService.submitReview(10L, new ReviewRequest(5, 5000L), 1L);

        assertNotNull(result);
        assertEquals(10L, result.cardId());
        verify(srsService).applyReview(progress, 5, 1L);
        verify(cardProgressRepository).save(progress);
        verify(reviewLogRepository).save(any(ReviewLog.class));
    }

    @Test
    void submitReview_withAiEvidence_savesAnswerFields() {
        when(cardService.getCardOrThrow(10L)).thenReturn(card);
        when(cardProgressRepository.findByCardId(10L)).thenReturn(Optional.of(progress));

        reviewService.submitReview(10L,
                new ReviewRequest(4, 7000L, "RDB 和 AOF", "B", "基本正确", "RDB 快照加 AOF 日志"),
                1L);

        ArgumentCaptor<ReviewLog> captor = ArgumentCaptor.forClass(ReviewLog.class);
        verify(reviewLogRepository).save(captor.capture());
        ReviewLog saved = captor.getValue();
        assertEquals("RDB 和 AOF", saved.getUserAnswer());
        assertEquals("B", saved.getAiGrade());
        assertEquals("基本正确", saved.getAiFeedback());
        assertEquals("RDB 快照加 AOF 日志", saved.getAiSuggestedAnswer());
    }

    @Test
    void evaluateAnswer_returnsStructuredAiEvaluationWithoutScheduling() {
        AnswerEvaluationDto evaluation = new AnswerEvaluationDto(
                "B", 4, 82, "Good", List.of("tradeoff"), "Suggested");
        when(cardService.getCardOrThrow(10L)).thenReturn(card);
        when(cardProgressRepository.findByCardId(10L)).thenReturn(Optional.of(progress));
        when(aiService.evaluateFlashcardAnswer("front", "back", "answer", false, 1L)).thenReturn(evaluation);

        AnswerEvaluationDto result = reviewService.evaluateAnswer(
                10L, new AnswerEvaluationRequest("answer"), 1L);

        assertEquals("B", result.grade());
        assertEquals(4, result.quality());
        verify(srsService, never()).applyReview(any(), anyInt(), anyLong());
        verify(reviewLogRepository, never()).save(any());
    }

    @Test
    void submitReview_firstLearning_boundsAiReviewInterval() {
        progress.setLastReviewDate(null);
        progress.setFirstLearnedDate(null);
        when(cardService.getCardOrThrow(10L)).thenReturn(card);
        when(cardProgressRepository.findByCardId(10L)).thenReturn(Optional.of(progress));

        reviewService.submitReview(
                10L,
                new ReviewRequest(2, 5000L, "partial", "D", "needs work", "answer", 10),
                1L);

        assertEquals(1, progress.getInterval());
        assertEquals(LocalDate.now().plusDays(1), progress.getNextReviewDate());
        ArgumentCaptor<ReviewLog> captor = ArgumentCaptor.forClass(ReviewLog.class);
        verify(reviewLogRepository).save(captor.capture());
        assertEquals("LEARNING", captor.getValue().getLearningMode());
        assertEquals(10, captor.getValue().getAiRecommendedReviewDays());
    }

    @Test
    void submitReview_cardNotFound_throws() {
        when(cardService.getCardOrThrow(99L)).thenThrow(new NoSuchElementException("Not found"));
        assertThrows(NoSuchElementException.class,
                () -> reviewService.submitReview(99L, new ReviewRequest(5, null), 1L));
    }

    @Test
    void submitReview_noProgressRecord_throws() {
        when(cardService.getCardOrThrow(10L)).thenReturn(card);
        when(cardProgressRepository.findByCardId(10L)).thenReturn(Optional.empty());
        assertThrows(NoSuchElementException.class,
                () -> reviewService.submitReview(10L, new ReviewRequest(3, null), 1L));
    }

    @Test
    void undoLastReview_restoresPreviousState() {
        ReviewLog log = new ReviewLog(card, 5, 3000L);
        log.setPrevRepetitions(1);
        log.setPrevEaseFactor(2.3);
        log.setPrevStability(3.5);
        log.setPrevDifficulty(6.2);
        log.setPrevInterval(3);
        log.setPrevNextReviewDate(LocalDate.now().plusDays(3));
        log.setPrevLastReviewDate(LocalDate.now().minusDays(3));
        log.setPrevFirstLearnedDate(LocalDate.now().minusDays(10));

        when(cardService.getCardOrThrow(10L)).thenReturn(card);
        when(cardProgressRepository.findByCardId(10L)).thenReturn(Optional.of(progress));
        when(reviewLogRepository.findLatestByCardId(10L)).thenReturn(Optional.of(log));

        ReviewCardDto result = reviewService.undoLastReview(10L, 1L);

        assertNotNull(result);
        assertEquals(1, progress.getRepetitions());
        assertEquals(2.3, progress.getEaseFactor(), 1e-9);
        assertEquals(3.5, progress.getStability(), 1e-9);
        assertEquals(6.2, progress.getDifficulty(), 1e-9);
        assertEquals(3, progress.getInterval());
        verify(cardProgressRepository).save(progress);
        verify(reviewLogRepository).delete(log);
    }

    @Test
    void undoLastReview_noLog_throws() {
        when(cardService.getCardOrThrow(10L)).thenReturn(card);
        when(cardProgressRepository.findByCardId(10L)).thenReturn(Optional.of(progress));
        when(reviewLogRepository.findLatestByCardId(10L)).thenReturn(Optional.empty());
        assertThrows(NoSuchElementException.class,
                () -> reviewService.undoLastReview(10L, 1L));
    }

    @Test
    void getTodaysDueCards_empty_returnsEmptyList() {
        when(cardProgressRepository.findDueByUserId(anyLong(), any(LocalDate.class)))
                .thenReturn(List.of());
        List<ReviewCardDto> result = reviewService.getTodaysDueCards(1L);
        assertTrue(result.isEmpty());
    }

    @Test
    void getTodaysDueCards_withCards_appliesDeckLimit() {
        progress.setLastReviewDate(null); // new card
        when(cardProgressRepository.findDueByUserId(1L, LocalDate.now()))
                .thenReturn(List.of(progress));
        deck.setDailyReviewLimit(5);
        deck.setDailyNewCardLimit(3);
        when(deckRepository.findById(1L)).thenReturn(Optional.of(deck));
        when(reviewLogRepository.countByDeckIdAndReviewDate(1L, LocalDate.now())).thenReturn(0L);
        when(cardProgressRepository.countFirstLearnedTodayByDeckId(1L, LocalDate.now())).thenReturn(0L);

        List<ReviewCardDto> result = reviewService.getTodaysDueCards(1L);
        assertEquals(1, result.size());
        assertTrue(result.get(0).isNew());
    }
}
