package com.syncflow.backend.controller;

import com.syncflow.backend.dto.CardSummaryResponse;
import com.syncflow.backend.dto.CreateCardRequest;
import com.syncflow.backend.dto.MoveCardRequest;
import com.syncflow.backend.dto.SetCardAssigneesRequest;
import com.syncflow.backend.dto.SetCardLabelsRequest;
import com.syncflow.backend.dto.UpdateCardRequest;
import com.syncflow.backend.service.CardService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.security.Principal;

@RestController
public class CardController {
    private final CardService cardService;

    public CardController(CardService cardService) {
        this.cardService = cardService;
    }

    @PostMapping("/api/v1/lists/{listId}/cards")
    public ResponseEntity<CardSummaryResponse> create(Principal principal, @PathVariable Long listId,
                                                      @Valid @RequestBody CreateCardRequest request) {
        CardSummaryResponse card = cardService.create(principal.getName(), listId, request);
        return ResponseEntity.created(URI.create("/api/v1/cards/" + card.id())).body(card);
    }

    @PutMapping("/api/v1/cards/{cardId}")
    public CardSummaryResponse update(Principal principal, @PathVariable Long cardId,
                                     @Valid @RequestBody UpdateCardRequest request) {
        return cardService.update(principal.getName(), cardId, request);
    }

    @DeleteMapping("/api/v1/cards/{cardId}")
    public ResponseEntity<Void> delete(Principal principal, @PathVariable Long cardId) {
        cardService.delete(principal.getName(), cardId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/api/v1/cards/{cardId}/move")
    public CardSummaryResponse move(Principal principal, @PathVariable Long cardId,
                                    @Valid @RequestBody MoveCardRequest request) {
        return cardService.move(principal.getName(), cardId, request);
    }

    @PutMapping("/api/v1/cards/{cardId}/assignees")
    public CardSummaryResponse setAssignees(Principal principal, @PathVariable Long cardId,
                                            @Valid @RequestBody SetCardAssigneesRequest request) {
        return cardService.setAssignees(principal.getName(), cardId, request);
    }

    @PutMapping("/api/v1/cards/{cardId}/labels")
    public CardSummaryResponse setLabels(Principal principal, @PathVariable Long cardId,
                                        @Valid @RequestBody SetCardLabelsRequest request) {
        return cardService.setLabels(principal.getName(), cardId, request);
    }
}
