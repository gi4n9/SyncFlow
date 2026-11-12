package com.syncflow.backend.controller;

import com.syncflow.backend.dto.PageResponse;
import com.syncflow.backend.dto.SearchCardResponse;
import com.syncflow.backend.model.CardPriority;
import com.syncflow.backend.service.CardSearchService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;
import java.security.Principal;
import java.time.Instant;

@RestController
@RequestMapping("/api/v1/search/cards")
public class CardSearchController {
    private final CardSearchService searchService;
    public CardSearchController(CardSearchService searchService) { this.searchService = searchService; }

    @GetMapping
    public PageResponse<SearchCardResponse> search(Principal principal,
            @RequestParam(required = false) Long boardId,
            @RequestParam(required = false) Long workspaceId,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Long assigneeId,
            @RequestParam(required = false) String label,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant dueFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant dueTo,
            @RequestParam(required = false) CardPriority priority,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return searchService.search(principal.getName(), boardId, workspaceId, q, assigneeId, label,
            dueFrom, dueTo, priority, page, size);
    }
}
