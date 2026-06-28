package com.memospark.core.controller;

import com.memospark.core.config.CurrentUser;
import com.memospark.core.config.UserPrincipal;
import com.memospark.core.dto.*;
import com.memospark.core.service.CardService;
import com.memospark.core.service.DeckService;
import com.memospark.core.service.SpacedRepetitionService;
import com.memospark.core.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/decks")
@RequiredArgsConstructor
public class DeckController {

    private final DeckService deckService;
    private final CardService cardService;
    private final UserService userService;
    private final SpacedRepetitionService srsService;

    // ── Pool endpoints ──

    @GetMapping("/pool")
    public List<DeckSummaryDto> getPoolDecks() {
        return deckService.getPoolDecks();
    }

    @PostMapping("/pool")
    @ResponseStatus(HttpStatus.CREATED)
    public DeckSummaryDto createPoolDeck(@RequestBody CreateDeckRequest req, @CurrentUser UserPrincipal principal) {
        var user = userService.getUserByUsername(principal.username());
        return deckService.createPoolDeck(user, req);
    }

    @PostMapping("/pool/{id}/copy")
    @ResponseStatus(HttpStatus.CREATED)
    public DeckSummaryDto copyPoolDeck(@PathVariable Long id, @CurrentUser UserPrincipal principal) {
        var user = userService.getUserByUsername(principal.username());
        return deckService.copyPoolDeck(id, user, srsService);
    }

    @GetMapping
    public List<DeckSummaryDto> getAllDecks(@CurrentUser UserPrincipal principal) {
        return deckService.getAllDecks(principal.id(), principal.admin());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public DeckSummaryDto createDeck(@RequestBody CreateDeckRequest req, @CurrentUser UserPrincipal principal) {
        var user = userService.getUserByUsername(principal.username());
        return deckService.createDeck(user, req);
    }

    @PutMapping("/{id}")
    public DeckSummaryDto updateDeck(@PathVariable Long id, @RequestBody UpdateDeckRequest req,
                                      @CurrentUser UserPrincipal principal) {
        var deck = deckService.getDeckOrThrow(id);
        deckService.verifyOwnership(deck, principal.id(), principal.admin());
        return deckService.updateDeck(id, req);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteDeck(@PathVariable Long id, @CurrentUser UserPrincipal principal) {
        var deck = deckService.getDeckOrThrow(id);
        deckService.verifyOwnership(deck, principal.id(), principal.admin());
        deckService.deleteDeck(id);
    }

    @GetMapping("/{id}/tags")
    public List<String> getTags(@PathVariable Long id, @CurrentUser UserPrincipal principal) {
        var deck = deckService.getDeckOrThrow(id);
        deckService.verifyOwnership(deck, principal.id(), principal.admin());
        return deckService.getDistinctTags(id);
    }

    // --- Card endpoints nested under deck ---

    @GetMapping("/{id}/cards")
    public List<ReviewCardDto> getCards(@PathVariable Long id, @CurrentUser UserPrincipal principal) {
        return cardService.getCardsByDeck(id, principal.id(), principal.admin());
    }

    @PostMapping("/{id}/cards")
    @ResponseStatus(HttpStatus.CREATED)
    public ReviewCardDto createCard(@PathVariable Long id, @RequestBody CreateCardRequest req,
                                     @CurrentUser UserPrincipal principal) {
        return cardService.createCard(id, req, principal.id(), principal.admin());
    }

    @PostMapping("/{id}/cards/from-text")
    @ResponseStatus(HttpStatus.CREATED)
    public List<ReviewCardDto> generateCardsFromText(@PathVariable Long id,
                                                     @RequestBody GenerateCardsFromTextRequest req,
                                                     @CurrentUser UserPrincipal principal) {
        return cardService.generateCardsFromText(
                id,
                req.text(),
                req.count(),
                req.language(),
                principal.id(),
                principal.admin());
    }

    @PutMapping("/{id}/cards/{cid}")
    public ReviewCardDto updateCard(@PathVariable Long id, @PathVariable Long cid,
                                     @RequestBody CreateCardRequest req,
                                     @CurrentUser UserPrincipal principal) {
        return cardService.updateCard(id, cid, req, principal.id(), principal.admin());
    }

    @DeleteMapping("/{id}/cards/{cid}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteCard(@PathVariable Long id, @PathVariable Long cid, @CurrentUser UserPrincipal principal) {
        cardService.deleteCard(id, cid, principal.id(), principal.admin());
    }

    @PostMapping("/{id}/cards/batch-delete")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void batchDeleteCards(@PathVariable Long id, @RequestBody BatchCardRequest req,
                                  @CurrentUser UserPrincipal principal) {
        cardService.batchDelete(id, req.cardIds(), principal.id(), principal.admin());
    }

    @PostMapping("/{id}/cards/batch-move")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void batchMoveCards(@PathVariable Long id, @RequestBody BatchCardRequest req,
                                @CurrentUser UserPrincipal principal) {
        cardService.batchMove(id, req.targetDeckId(), req.cardIds(), principal.id(), principal.admin());
    }
}
