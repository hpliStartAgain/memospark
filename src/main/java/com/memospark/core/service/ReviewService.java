package com.memospark.core.service;

import com.memospark.core.domain.Card;
import com.memospark.core.domain.CardProgress;
import com.memospark.core.domain.Deck;
import com.memospark.core.domain.ReviewLog;
import com.memospark.core.dto.ReviewCardDto;
import com.memospark.core.dto.ReviewRequest;
import com.memospark.core.repository.CardProgressRepository;
import com.memospark.core.repository.DeckRepository;
import com.memospark.core.repository.ReviewLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReviewService {

    private final CardService cardService;
    private final CardProgressRepository cardProgressRepository;
    private final ReviewLogRepository reviewLogRepository;
    private final DeckRepository deckRepository;
    private final DeckService deckService;
    private final SpacedRepetitionService srsService;

    @Transactional(readOnly = true)
    public List<ReviewCardDto> getTodaysDueCards(Long userId) {
        LocalDate today = LocalDate.now();
        List<CardProgress> allDue = cardProgressRepository.findDueByUserId(userId, today);

        // Group by deck and apply per-deck limit
        Map<Long, List<CardProgress>> byDeck = allDue.stream()
                .collect(Collectors.groupingBy(cp -> cp.getCard().getDeck().getId()));

        List<ReviewCardDto> result = new ArrayList<>();
        for (var entry : byDeck.entrySet()) {
            Long deckId = entry.getKey();
            List<CardProgress> dueCards = entry.getValue();
            result.addAll(applyLimit(deckId, dueCards, today));
        }
        return result;
    }

    @Transactional(readOnly = true)
    public List<ReviewCardDto> getDueCardsByDeck(Long deckId, List<String> tags, Long userId, boolean isAdmin) {
        Deck deck = deckService.getDeckOrThrow(deckId);
        deckService.verifyOwnership(deck, userId, isAdmin);
        LocalDate today = LocalDate.now();
        List<CardProgress> dueCards = cardProgressRepository.findDueByDeckId(deckId, today);

        // Filter by tags if provided
        if (tags != null && !tags.isEmpty()) {
            dueCards = dueCards.stream()
                    .filter(cp -> {
                        String cardTags = cp.getCard().getTags();
                        if (cardTags == null || cardTags.isBlank()) return false;
                        var cardTagSet = java.util.Arrays.stream(cardTags.split(","))
                                .map(String::trim).collect(Collectors.toSet());
                        return tags.stream().anyMatch(cardTagSet::contains);
                    })
                    .toList();
        }

        return applyLimit(deckId, dueCards, today);
    }

    private List<ReviewCardDto> applyLimit(Long deckId, List<CardProgress> dueCards, LocalDate today) {
        Deck deck = deckRepository.findById(deckId).orElse(null);
        Integer reviewLimit = deck != null ? deck.getDailyReviewLimit() : null;
        Integer newCardLimit = deck != null ? deck.getDailyNewCardLimit() : null;

        // Separate review cards and new cards
        List<CardProgress> reviewCards = dueCards.stream()
                .filter(cp -> cp.getLastReviewDate() != null)
                .toList();
        List<CardProgress> newCards = dueCards.stream()
                .filter(cp -> cp.getLastReviewDate() == null)
                .toList();

        // Apply daily new card limit
        if (newCardLimit != null && newCardLimit > 0) {
            long newLearnedToday = cardProgressRepository.countFirstLearnedTodayByDeckId(deckId, today);
            long newRemaining = Math.max(0, newCardLimit - newLearnedToday);
            newCards = newCards.stream().limit(newRemaining).toList();
        }

        // Combine: review cards first, then new cards
        List<CardProgress> combined = new ArrayList<>(reviewCards);
        combined.addAll(newCards);

        // Apply overall daily review limit
        if (reviewLimit != null && reviewLimit > 0) {
            long reviewed = reviewLogRepository.countByDeckIdAndReviewDate(deckId, today);
            long remaining = reviewLimit - reviewed;
            if (remaining <= 0) return List.of();
            combined = combined.stream().limit(remaining).toList();
        }

        return combined.stream()
                .map(cp -> toReviewCardDto(cp.getCard(), cp))
                .toList();
    }

    @Transactional
    public ReviewCardDto submitReview(Long cardId, ReviewRequest req, Long userId) {
        Card card = cardService.getCardOrThrow(cardId);
        // Verify ownership through deck
        deckService.verifyOwnership(card.getDeck(), userId, false);
        CardProgress progress = cardProgressRepository.findByCardId(cardId)
                .orElseThrow(() -> new NoSuchElementException("No progress record for card: " + cardId));

        // Snapshot before review (for undo)
        ReviewLog log = new ReviewLog(card, req.quality(), req.timeSpentMs());
        log.setPrevRepetitions(progress.getRepetitions());
        log.setPrevEaseFactor(progress.getEaseFactor());
        log.setPrevStability(progress.getStability());
        log.setPrevDifficulty(progress.getDifficulty());
        log.setPrevInterval(progress.getInterval());
        log.setPrevNextReviewDate(progress.getNextReviewDate());
        log.setPrevLastReviewDate(progress.getLastReviewDate());
        log.setPrevFirstLearnedDate(progress.getFirstLearnedDate());

        srsService.applyReview(progress, req.quality(), userId);
        cardProgressRepository.save(progress);
        reviewLogRepository.save(log);

        return toReviewCardDto(card, progress);
    }

    @Transactional
    public ReviewCardDto undoLastReview(Long cardId, Long userId) {
        Card card = cardService.getCardOrThrow(cardId);
        deckService.verifyOwnership(card.getDeck(), userId, false);
        CardProgress progress = cardProgressRepository.findByCardId(cardId)
                .orElseThrow(() -> new NoSuchElementException("No progress record for card: " + cardId));

        ReviewLog lastLog = reviewLogRepository.findLatestByCardId(cardId)
                .orElseThrow(() -> new NoSuchElementException("No review to undo for card: " + cardId));

        // Restore previous state
        progress.setRepetitions(lastLog.getPrevRepetitions());
        progress.setEaseFactor(lastLog.getPrevEaseFactor());
        progress.setStability(lastLog.getPrevStability());
        progress.setDifficulty(lastLog.getPrevDifficulty());
        progress.setInterval(lastLog.getPrevInterval());
        progress.setNextReviewDate(lastLog.getPrevNextReviewDate());
        progress.setLastReviewDate(lastLog.getPrevLastReviewDate());
        progress.setFirstLearnedDate(lastLog.getPrevFirstLearnedDate());
        cardProgressRepository.save(progress);

        // Delete the review log
        reviewLogRepository.delete(lastLog);

        return toReviewCardDto(card, progress);
    }

    @Transactional(readOnly = true)
    public List<ReviewCardDto> getHardCards(Long userId) {
        List<Object[]> rows = reviewLogRepository.findHardCardIdsByUserId(userId, 2L);
        List<Long> cardIds = rows.stream().map(r -> (Long) r[0]).toList();

        return cardIds.stream()
                .map(cardId -> {
                    try {
                        Card card = cardService.getCardOrThrow(cardId);
                        CardProgress progress = cardProgressRepository.findByCardId(cardId).orElse(null);
                        if (progress == null) return null;
                        return toReviewCardDto(card, progress);
                    } catch (Exception e) { return null; }
                })
                .filter(dto -> dto != null)
                .toList();
    }

    private ReviewCardDto toReviewCardDto(Card card, CardProgress progress) {
        return new ReviewCardDto(
                card.getId(),
                card.getDeck().getId(),
                card.getDeck().getName(),
                card.getFront(),
                card.getBack(),
                card.getTags(),
                progress.getRepetitions(),
                progress.getEaseFactor(),
                progress.getInterval(),
                progress.getNextReviewDate(),
                progress.getLastReviewDate() == null
        );
    }
}
