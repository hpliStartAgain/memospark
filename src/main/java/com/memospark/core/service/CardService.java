package com.memospark.core.service;

import com.memospark.core.domain.Card;
import com.memospark.core.domain.CardDifficulty;
import com.memospark.core.domain.CardProgress;
import com.memospark.core.domain.Deck;
import com.memospark.core.domain.LearningStage;
import com.memospark.core.dto.CardGovernanceResultDto;
import com.memospark.core.dto.CreateCardRequest;
import com.memospark.core.dto.ReviewCardDto;
import com.memospark.core.repository.CardProgressRepository;
import com.memospark.core.repository.CardRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
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
        applyLearningMetadata(card, req);
        if (req.stageOrder() == null) {
            card.setStageOrder((int) Math.min(Integer.MAX_VALUE, cardRepository.countByDeckId(deckId) + 1));
        }
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
        applyLearningMetadata(card, req);
        card = cardRepository.save(card);
        return toReviewCardDto(card, cardProgressRepository.findByCardId(card.getId()).orElse(null));
    }

    @Transactional
    public CardGovernanceResultDto governCards(
            Long deckId, String language, Long userId, boolean isAdmin) {
        Deck deck = deckService.getDeckOrThrow(deckId);
        deckService.verifyOwnership(deck, userId, isAdmin);
        List<Card> cards = cardRepository.findByDeckId(deckId);
        if (cards.isEmpty()) {
            return new CardGovernanceResultDto(0, "牌组中还没有卡片。", List.of());
        }

        Map<Long, Map<String, Object>> suggestions = new HashMap<>();
        for (int start = 0; start < cards.size(); start += 30) {
            List<Card> batch = cards.subList(start, Math.min(start + 30, cards.size()));
            List<Map<String, Object>> payload = batch.stream().map(card -> {
                Map<String, Object> row = new HashMap<>();
                row.put("cardId", card.getId());
                row.put("front", truncate(card.getFront(), 700));
                row.put("back", truncate(card.getBack(), 1200));
                row.put("tags", card.getTags());
                return row;
            }).toList();
            aiService.governFlashcards(deck.getName(), payload, language, userId)
                    .forEach(row -> {
                        Long cardId = asLong(row.get("cardId"));
                        if (cardId != null) suggestions.put(cardId, row);
                    });
        }

        int updated = 0;
        LocalDateTime now = LocalDateTime.now();
        for (Card card : cards) {
            Map<String, Object> row = suggestions.get(card.getId());
            if (row == null) continue;
            card.setContentDifficulty(parseDifficulty(asString(row.get("difficulty")), card.getContentDifficulty()));
            card.setLearningStage(parseStage(asString(row.get("stage")), card.getLearningStage()));
            card.setStageOrder(Math.max(1, asInt(row.get("order"), card.getStageOrder())));
            String normalizedTags = clean(asString(row.get("tags")));
            if (normalizedTags != null) card.setTags(normalizedTags);
            card.setGovernanceNote(truncate(clean(asString(row.get("rationale"))), 500));
            card.setGovernedAt(now);
            updated++;
        }
        cardRepository.saveAll(cards);

        List<ReviewCardDto> governedCards = getCardsByDeck(deckId, userId, isAdmin);
        String summary = "已按前置关系治理 " + updated + " 张卡片，并生成入门、进阶、实战顺序。";
        return new CardGovernanceResultDto(updated, summary, governedCards);
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
        card.setContentDifficulty(parseDifficulty(row.get("difficulty"), CardDifficulty.MEDIUM));
        card.setLearningStage(parseStage(row.get("stage"), LearningStage.FOUNDATION));
        card.setStageOrder(Math.max(1, asInt(row.get("order"),
                (int) Math.min(Integer.MAX_VALUE, cardRepository.countByDeckId(deck.getId()) + 1))));
        card.setGovernanceNote("AI 生成时已完成初始分级");
        card.setGovernedAt(LocalDateTime.now());
        card = cardRepository.save(card);
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
                card.getContentDifficulty().name(),
                card.getLearningStage().name(),
                card.getStageOrder(),
                card.getGovernanceNote(),
                progress != null ? progress.getRepetitions() : 0,
                progress != null ? progress.getEaseFactor() : 2.5,
                progress != null ? progress.getInterval() : 0,
                progress != null ? progress.getNextReviewDate() : null,
                progress == null || progress.getLastReviewDate() == null
        );
    }

    private void applyLearningMetadata(Card card, CreateCardRequest req) {
        if (req.contentDifficulty() != null) {
            card.setContentDifficulty(parseDifficulty(req.contentDifficulty(), card.getContentDifficulty()));
        }
        if (req.learningStage() != null) {
            card.setLearningStage(parseStage(req.learningStage(), card.getLearningStage()));
        }
        if (req.stageOrder() != null) {
            card.setStageOrder(Math.max(1, req.stageOrder()));
        }
    }

    private CardDifficulty parseDifficulty(String value, CardDifficulty fallback) {
        if (value == null || value.isBlank()) return fallback;
        try {
            return CardDifficulty.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return fallback;
        }
    }

    private LearningStage parseStage(String value, LearningStage fallback) {
        if (value == null || value.isBlank()) return fallback;
        try {
            return LearningStage.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return fallback;
        }
    }

    private Long asLong(Object value) {
        if (value instanceof Number number) return number.longValue();
        if (value instanceof String text) {
            try {
                return Long.parseLong(text.trim());
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    private int asInt(Object value, int fallback) {
        if (value instanceof Number number) return number.intValue();
        if (value instanceof String text) {
            try {
                return Integer.parseInt(text.trim());
            } catch (NumberFormatException ignored) {
                return fallback;
            }
        }
        return fallback;
    }

    private String asString(Object value) {
        return value == null ? null : String.valueOf(value);
    }
}
