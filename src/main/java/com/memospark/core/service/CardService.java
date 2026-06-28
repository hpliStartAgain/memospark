package com.memospark.core.service;

import com.memospark.core.domain.Card;
import com.memospark.core.domain.CardProgress;
import com.memospark.core.domain.Deck;
import com.memospark.core.dto.CreateCardRequest;
import com.memospark.core.dto.ReviewCardDto;
import com.memospark.core.repository.CardProgressRepository;
import com.memospark.core.repository.CardRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
public class CardService {

    private final CardRepository cardRepository;
    private final CardProgressRepository cardProgressRepository;
    private final DeckService deckService;
    private final SpacedRepetitionService srsService;
    private final AiService aiService;

    @Transactional(readOnly = true)
    public List<ReviewCardDto> getCardsByDeck(Long deckId, Long userId, boolean isAdmin) {
        Deck deck = deckService.getDeckOrThrow(deckId);
        deckService.verifyOwnership(deck, userId, isAdmin);
        List<Card> cards = cardRepository.findByDeckId(deckId);
        // Batch-fetch all progress in one query — no N+1
        List<Long> cardIds = cards.stream().map(Card::getId).toList();
        Map<Long, CardProgress> progressMap = cardProgressRepository
                .findByCardIdIn(cardIds).stream()
                .collect(java.util.stream.Collectors.toMap(cp -> cp.getCard().getId(), cp -> cp));
        return cards.stream()
                .map(c -> toReviewCardDto(c, progressMap.get(c.getId())))
                .toList();
    }

    @Transactional
    public ReviewCardDto createCard(Long deckId, CreateCardRequest req, Long userId, boolean isAdmin) {
        Deck deck = deckService.getDeckOrThrow(deckId);
        deckService.verifyOwnership(deck, userId, isAdmin);
        Card card = new Card(deck, req.front(), req.back(), req.tags());
        card = cardRepository.save(card);

        CardProgress progress = new CardProgress(card);
        srsService.initProgress(progress, userId);
        cardProgressRepository.save(progress);

        return toReviewCardDto(card, cardProgressRepository.findByCardId(card.getId()).orElse(null));
    }

    @Transactional
    public List<ReviewCardDto> generateCardsFromText(Long deckId, String text, Integer count,
                                                      String language, Long userId, boolean isAdmin) {
        Deck deck = deckService.getDeckOrThrow(deckId);
        deckService.verifyOwnership(deck, userId, isAdmin);
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("Text is required");
        }

        int cardCount = Math.max(1, Math.min(count != null ? count : 8, 30));
        String lang = language != null && !language.isBlank() ? language : "zh";
        String topic = """
                Extract atomic active-recall flashcards from this interview experience, note, or study material.
                Keep each card focused on one fact, decision, tradeoff, or interview answer point.
                Avoid recognition-only cards and avoid copying long passages verbatim.

                Source:
                %s
                """.formatted(truncate(text.trim(), 12000));

        List<Map<String, String>> generated = aiService.generateCards(topic, cardCount, lang, userId);
        return generated.stream()
                .map(row -> saveGeneratedCard(deck, row, userId))
                .toList();
    }

    @Transactional
    public ReviewCardDto updateCard(Long deckId, Long cardId, CreateCardRequest req, Long userId, boolean isAdmin) {
        Deck deck = deckService.getDeckOrThrow(deckId);
        deckService.verifyOwnership(deck, userId, isAdmin);
        Card card = getCardOrThrow(cardId);
        if (!card.getDeck().getId().equals(deckId)) {
            throw new NoSuchElementException("Card " + cardId + " not in deck " + deckId);
        }
        if (req.front() != null) card.setFront(req.front());
        if (req.back() != null) card.setBack(req.back());
        if (req.tags() != null) card.setTags(req.tags());
        card = cardRepository.save(card);
        return toReviewCardDto(card, cardProgressRepository.findByCardId(card.getId()).orElse(null));
    }

    @Transactional
    public void deleteCard(Long deckId, Long cardId, Long userId, boolean isAdmin) {
        Deck deck = deckService.getDeckOrThrow(deckId);
        deckService.verifyOwnership(deck, userId, isAdmin);
        Card card = getCardOrThrow(cardId);
        if (!card.getDeck().getId().equals(deckId)) {
            throw new NoSuchElementException("Card " + cardId + " not in deck " + deckId);
        }
        cardProgressRepository.deleteByCardId(cardId);
        cardRepository.delete(card);
    }

    @Transactional
    public void batchDelete(Long deckId, List<Long> cardIds, Long userId, boolean isAdmin) {
        Deck deck = deckService.getDeckOrThrow(deckId);
        deckService.verifyOwnership(deck, userId, isAdmin);
        for (Long cardId : cardIds) {
            Card card = cardRepository.findById(cardId).orElse(null);
            if (card != null && card.getDeck().getId().equals(deckId)) {
                cardProgressRepository.deleteByCardId(cardId);
                cardRepository.delete(card);
            }
        }
    }

    @Transactional
    public void batchMove(Long fromDeckId, Long toDeckId, List<Long> cardIds, Long userId, boolean isAdmin) {
        Deck fromDeck = deckService.getDeckOrThrow(fromDeckId);
        deckService.verifyOwnership(fromDeck, userId, isAdmin);
        Deck toDeck = deckService.getDeckOrThrow(toDeckId);
        deckService.verifyOwnership(toDeck, userId, isAdmin);
        for (Long cardId : cardIds) {
            Card card = cardRepository.findById(cardId).orElse(null);
            if (card != null && card.getDeck().getId().equals(fromDeckId)) {
                card.setDeck(toDeck);
                cardRepository.save(card);
            }
        }
    }

    public Card getCardOrThrow(Long cardId) {
        return cardRepository.findById(cardId)
                .orElseThrow(() -> new NoSuchElementException("Card not found: " + cardId));
    }

    private ReviewCardDto saveGeneratedCard(Deck deck, Map<String, String> row, Long userId) {
        String front = clean(row.get("front"));
        String back = clean(row.get("back"));
        if (front == null || back == null) {
            throw new RuntimeException("AI returned invalid card content");
        }
        String tags = clean(row.get("tags"));
        Card card = cardRepository.save(new Card(deck, front, back, tags != null ? tags : "from-text"));
        CardProgress progress = new CardProgress(card);
        srsService.initProgress(progress, userId);
        cardProgressRepository.save(progress);
        return toReviewCardDto(card, progress);
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

    private ReviewCardDto toReviewCardDto(Card card, CardProgress progress) {
        return new ReviewCardDto(
                card.getId(),
                card.getDeck().getId(),
                card.getDeck().getName(),
                card.getFront(),
                card.getBack(),
                card.getTags(),
                progress != null ? progress.getRepetitions() : 0,
                progress != null ? progress.getEaseFactor() : 2.5,
                progress != null ? progress.getInterval() : 0,
                progress != null ? progress.getNextReviewDate() : null,
                progress == null || progress.getLastReviewDate() == null
        );
    }
}
