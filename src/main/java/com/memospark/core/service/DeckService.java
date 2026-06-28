package com.memospark.core.service;

import com.memospark.core.domain.*;
import com.memospark.core.dto.CreateDeckRequest;
import com.memospark.core.dto.DeckSummaryDto;
import com.memospark.core.dto.UpdateDeckRequest;
import com.memospark.core.repository.CardProgressRepository;
import com.memospark.core.repository.CardRepository;
import com.memospark.core.repository.DeckRepository;
import com.memospark.core.repository.ReviewLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
public class DeckService {

    private final DeckRepository deckRepository;
    private final CardRepository cardRepository;
    private final CardProgressRepository cardProgressRepository;
    private final ReviewLogRepository reviewLogRepository;

    @Transactional(readOnly = true)
    public List<DeckSummaryDto> getAllDecks(Long userId, boolean isAdmin) {
        LocalDate today = LocalDate.now();
        var decks = isAdmin ? deckRepository.findAll() : deckRepository.findByUserId(userId);
        return decks.stream()
                .filter(deck -> deck.getType() != DeckType.POOL) // pool decks managed separately
                .map(deck -> toSummary(deck, today))
                .toList();
    }

    @Transactional(readOnly = true)
    public DeckSummaryDto getDeckSummary(Long id) {
        Deck deck = getDeckOrThrow(id);
        return toSummary(deck, LocalDate.now());
    }

    @Transactional
    public DeckSummaryDto createDeck(User user, CreateDeckRequest req) {
        Deck deck = new Deck(req.name(), req.description(), DeckType.CUSTOM, user);
        deck.setDailyReviewLimit(req.dailyReviewLimit());
        deck.setDailyNewCardLimit(req.dailyNewCardLimit());
        deck = deckRepository.save(deck);
        return toSummary(deck, LocalDate.now());
    }

    @Transactional
    public DeckSummaryDto updateDeck(Long id, UpdateDeckRequest req) {
        Deck deck = getDeckOrThrow(id);
        if (req.name() != null) deck.setName(req.name());
        if (req.description() != null) deck.setDescription(req.description());
        if (req.dailyReviewLimit() != null) deck.setDailyReviewLimit(req.dailyReviewLimit());
        if (req.dailyNewCardLimit() != null) deck.setDailyNewCardLimit(req.dailyNewCardLimit());
        deck = deckRepository.save(deck);
        return toSummary(deck, LocalDate.now());
    }

    @Transactional
    public void deleteDeck(Long id) {
        Deck deck = getDeckOrThrow(id);
        // Delete review logs, progress, cards (no cascade defined)
        cardRepository.findByDeckId(id).forEach(card -> {
            reviewLogRepository.deleteByCardId(card.getId());
            cardProgressRepository.deleteByCardId(card.getId());
        });
        cardRepository.deleteByDeckId(id);
        deckRepository.delete(deck);
    }

    public List<String> getDistinctTags(Long deckId) {
        getDeckOrThrow(deckId);
        return cardRepository.findDistinctTagsByDeckId(deckId).stream()
                .flatMap(tags -> java.util.Arrays.stream(tags.split(",")))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .distinct()
                .sorted()
                .toList();
    }

    public Deck getDeckOrThrow(Long id) {
        return deckRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Deck not found: " + id));
    }

    /** Admin creates a pool deck */
    @Transactional
    public DeckSummaryDto createPoolDeck(User admin, CreateDeckRequest req) {
        Deck deck = new Deck(req.name(), req.description(), DeckType.POOL, admin);
        deck = deckRepository.save(deck);
        return toSummary(deck, LocalDate.now());
    }

    /** List all pool decks (for users to browse) */
    @Transactional(readOnly = true)
    public List<DeckSummaryDto> getPoolDecks() {
        LocalDate today = LocalDate.now();
        return deckRepository.findByType(DeckType.POOL).stream()
                .map(deck -> toSummary(deck, today))
                .toList();
    }

    /** User copies a pool deck to their own collection */
    @Transactional
    public DeckSummaryDto copyPoolDeck(Long poolDeckId, User user, SpacedRepetitionService srsService) {
        Deck source = getDeckOrThrow(poolDeckId);
        if (source.getType() != DeckType.POOL) {
            throw new IllegalArgumentException("Deck is not a pool deck");
        }

        Deck copy = new Deck(source.getName(), source.getDescription(), DeckType.CUSTOM, user);
        copy = deckRepository.save(copy);

        for (Card srcCard : cardRepository.findByDeckId(poolDeckId)) {
            Card card = new Card(copy, srcCard.getFront(), srcCard.getBack(), srcCard.getTags());
            card.setContentDifficulty(srcCard.getContentDifficulty());
            card.setLearningStage(srcCard.getLearningStage());
            card.setStageOrder(srcCard.getStageOrder());
            card.setGovernanceNote(srcCard.getGovernanceNote());
            card.setGovernedAt(srcCard.getGovernedAt());
            card = cardRepository.save(card);
            CardProgress progress = new CardProgress(card);
            srsService.initProgress(progress, user.getId());
            cardProgressRepository.save(progress);
        }

        return toSummary(copy, LocalDate.now());
    }

    /** Verify that the deck belongs to the given user (admins skip check) */
    public void verifyOwnership(Deck deck, Long userId, boolean isAdmin) {
        if (!isAdmin && (deck.getUser() == null || !deck.getUser().getId().equals(userId))) {
            throw new SecurityException("Access denied to deck: " + deck.getId());
        }
    }

    private DeckSummaryDto toSummary(Deck deck, LocalDate today) {
        long total = cardRepository.countByDeckId(deck.getId());
        long reviewCards = cardProgressRepository.countReviewDueByDeckId(deck.getId(), today);
        long totalNewDue = cardProgressRepository.countNewDueByDeckId(deck.getId(), today);
        long newLearnedToday = cardProgressRepository.countFirstLearnedTodayByDeckId(deck.getId(), today);
        long reviewed = reviewLogRepository.countByDeckIdAndReviewDate(deck.getId(), today);

        // newCards = today's new card quota remaining
        Integer newLimit = deck.getDailyNewCardLimit();
        long newCards;
        if (newLimit != null && newLimit > 0) {
            long remaining = Math.max(0, newLimit - newLearnedToday);
            newCards = Math.min(remaining, totalNewDue);
        } else {
            newCards = totalNewDue;
        }

        long due = reviewCards + newCards;

        String ownerUsername = deck.getUser() != null ? deck.getUser().getUsername() : null;
        return new DeckSummaryDto(
                deck.getId(), deck.getName(), deck.getDescription(),
                deck.getType(), deck.getCreatedAt(), total, due,
                newCards, reviewCards, newLearnedToday,
                deck.getDailyReviewLimit(), deck.getDailyNewCardLimit(),
                reviewed, ownerUsername);
    }
}
