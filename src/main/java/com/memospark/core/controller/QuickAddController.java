package com.memospark.core.controller;

import com.memospark.core.domain.Deck;
import com.memospark.core.domain.DeckType;
import com.memospark.core.domain.User;
import com.memospark.core.dto.*;
import com.memospark.core.repository.DeckRepository;
import com.memospark.core.repository.UserRepository;
import com.memospark.core.service.CardService;
import com.memospark.core.service.DeckService;
import com.memospark.core.service.ReviewService;
import com.memospark.core.service.StatisticsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

/**
 * Stateless Quick-Add API for external integrations (e.g. MCP server, scripts).
 * <p>
 * Authenticate with {@code Authorization: Bearer $MEMOSPARK_API_KEY}.
 * CSRF is disabled for this path (Bearer is stateless).
 */
@RestController
@RequestMapping("/api/quick-add")
@RequiredArgsConstructor
@Slf4j
public class QuickAddController {

    @Value("${quick.add.api.key:}")
    private String configuredApiKey;

    private final DeckRepository deckRepository;
    private final UserRepository userRepository;
    private final CardService cardService;
    private final DeckService deckService;
    private final StatisticsService statisticsService;
    private final ReviewService reviewService;

    // ── Auth helper ────────────────────────────────────────────────────────

    private User authenticate(String authHeader, String username) {
        if (configuredApiKey.isBlank()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "MEMOSPARK_API_KEY is not configured on the server");
        }
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                    "Missing or malformed Authorization header (expected: Bearer <key>)");
        }
        if (!authHeader.substring(7).equals(configuredApiKey)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid API key");
        }
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "User not found: " + username));
    }

    // ── Endpoints ──────────────────────────────────────────────────────────

    /**
     * Lists all CUSTOM decks for the given user.
     *
     * <pre>GET /api/quick-add/decks?username=alice
     * Authorization: Bearer &lt;key&gt;</pre>
     */
    @GetMapping("/decks")
    public List<Map<String, Object>> listDecks(
            @RequestHeader("Authorization") String auth,
            @RequestParam String username) {
        User user = authenticate(auth, username);
        return deckRepository.findByUserId(user.getId()).stream()
                .filter(d -> d.getType() != DeckType.POOL)
                .map(d -> Map.<String, Object>of(
                        "id", d.getId(),
                        "name", d.getName(),
                        "description", d.getDescription() != null ? d.getDescription() : ""))
                .toList();
    }

    /**
     * Adds a flashcard to the named deck (creates the deck if it does not exist).
     *
     * <pre>POST /api/quick-add/card
     * Authorization: Bearer &lt;key&gt;
     * { "username":"alice", "deckName":"AI问答", "front":"Q", "back":"A", "tags":"ai" }</pre>
     */
    @PostMapping("/card")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> addCard(
            @RequestHeader("Authorization") String auth,
            @Valid @RequestBody QuickAddCardRequest req) {
        User user = authenticate(auth, req.username());

        // Find or create deck by name (CUSTOM type)
        Deck deck = deckRepository
                .findByUserIdAndNameAndType(user.getId(), req.deckName(), DeckType.CUSTOM)
                .orElseGet(() -> {
                    Deck d = new Deck(req.deckName(), "", DeckType.CUSTOM, user);
                    log.info("Quick-add: created new deck '{}' for user '{}'",
                            req.deckName(), req.username());
                    return deckRepository.save(d);
                });

        ReviewCardDto card = cardService.createCard(
                deck.getId(),
                new CreateCardRequest(req.front(), req.back(), req.tags()),
                user.getId(),
                false);

        log.info("Quick-add: card #{} added to deck '{}' for user '{}'",
                card.cardId(), deck.getName(), req.username());

        return Map.of(
                "cardId", card.cardId(),
                "deckId", deck.getId(),
                "deckName", deck.getName(),
                "front", card.front(),
                "back", card.back(),
                "tags", card.tags() != null ? card.tags() : ""
        );
    }

    // ── Deck management ────────────────────────────────────────────────────

    /**
     * Get summary of a single deck (stats, due counts, limits).
     *
     * <pre>GET /api/quick-add/decks/{id}?username=alice</pre>
     */
    @GetMapping("/decks/{id}")
    public DeckSummaryDto getDeck(
            @RequestHeader("Authorization") String auth,
            @PathVariable Long id,
            @RequestParam String username) {
        User user = authenticate(auth, username);
        Deck deck = deckService.getDeckOrThrow(id);
        deckService.verifyOwnership(deck, user.getId(), false);
        return deckService.getDeckSummary(id);
    }

    /**
     * Get all cards in a deck.
     *
     * <pre>GET /api/quick-add/decks/{id}/cards?username=alice</pre>
     */
    @GetMapping("/decks/{id}/cards")
    public List<ReviewCardDto> getCards(
            @RequestHeader("Authorization") String auth,
            @PathVariable Long id,
            @RequestParam String username) {
        User user = authenticate(auth, username);
        return cardService.getCardsByDeck(id, user.getId(), false);
    }

    /**
     * Create a new deck.
     *
     * <pre>POST /api/quick-add/decks
     * { "username":"alice", "name":"新牌组", "description":"...", "dailyNewCardLimit":20 }</pre>
     */
    @PostMapping("/decks")
    @ResponseStatus(HttpStatus.CREATED)
    public DeckSummaryDto createDeck(
            @RequestHeader("Authorization") String auth,
            @Valid @RequestBody QuickAddDeckRequest req) {
        User user = authenticate(auth, req.username());
        return deckService.createDeck(user,
                new CreateDeckRequest(req.name(), req.description(),
                        req.dailyReviewLimit(), req.dailyNewCardLimit()));
    }

    /**
     * Update deck name/description/limits.
     *
     * <pre>PUT /api/quick-add/decks/{id}
     * { "username":"alice", "name":"新名字" }</pre>
     */
    @PutMapping("/decks/{id}")
    public DeckSummaryDto updateDeck(
            @RequestHeader("Authorization") String auth,
            @PathVariable Long id,
            @Valid @RequestBody QuickAddDeckRequest req) {
        User user = authenticate(auth, req.username());
        Deck deck = deckService.getDeckOrThrow(id);
        deckService.verifyOwnership(deck, user.getId(), false);
        return deckService.updateDeck(id,
                new UpdateDeckRequest(req.name(), req.description(),
                        req.dailyReviewLimit(), req.dailyNewCardLimit()));
    }

    /**
     * Delete a deck and all its cards.
     *
     * <pre>DELETE /api/quick-add/decks/{id}?username=alice</pre>
     */
    @DeleteMapping("/decks/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteDeck(
            @RequestHeader("Authorization") String auth,
            @PathVariable Long id,
            @RequestParam String username) {
        User user = authenticate(auth, username);
        Deck deck = deckService.getDeckOrThrow(id);
        deckService.verifyOwnership(deck, user.getId(), false);
        deckService.deleteDeck(id);
        log.info("Quick-add: deleted deck #{} for user '{}'", id, username);
    }

    // ── Read-only queries ──────────────────────────────────────────────────

    /**
     * Overall review statistics (total cards, streak, retention rate, etc.).
     *
     * <pre>GET /api/quick-add/stats?username=alice</pre>
     */
    @GetMapping("/stats")
    public StatsDto getStats(
            @RequestHeader("Authorization") String auth,
            @RequestParam String username) {
        User user = authenticate(auth, username);
        return statisticsService.getStats(user.getId());
    }

    /**
     * Daily review statistics for the last N days (default 30, max 90).
     *
     * <pre>GET /api/quick-add/stats/daily?username=alice&days=30</pre>
     */
    @GetMapping("/stats/daily")
    public List<DailyStatsDto> getDailyStats(
            @RequestHeader("Authorization") String auth,
            @RequestParam String username,
            @RequestParam(defaultValue = "30") int days) {
        User user = authenticate(auth, username);
        return statisticsService.getDailyStats(user.getId(), Math.min(days, 90));
    }

    /**
     * All cards due for review today (across all decks).
     *
     * <pre>GET /api/quick-add/review/today?username=alice</pre>
     */
    @GetMapping("/review/today")
    public List<ReviewCardDto> getTodayDue(
            @RequestHeader("Authorization") String auth,
            @RequestParam String username) {
        User user = authenticate(auth, username);
        return reviewService.getTodaysDueCards(user.getId());
    }

    /**
     * Cards due for review today in a specific deck.
     *
     * <pre>GET /api/quick-add/review/deck/{deckId}?username=alice</pre>
     */
    @GetMapping("/review/deck/{deckId}")
    public List<ReviewCardDto> getDuByDeck(
            @RequestHeader("Authorization") String auth,
            @PathVariable Long deckId,
            @RequestParam String username) {
        User user = authenticate(auth, username);
        return reviewService.getDueCardsByDeck(deckId, null, user.getId(), false);
    }
}
