package com.syncflow.backend.controller;

import com.syncflow.backend.dto.*;
import com.syncflow.backend.service.BoardService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.security.Principal;
import java.util.List;

@RestController
public class BoardController {
    private final BoardService boardService;

    public BoardController(BoardService boardService) {
        this.boardService = boardService;
    }

    @GetMapping("/api/v1/workspaces/{workspaceId}/boards")
    public List<BoardSummaryResponse> list(Principal principal, @PathVariable Long workspaceId,
                                           @RequestParam(defaultValue = "false") boolean includeArchived) {
        return boardService.list(principal.getName(), workspaceId, includeArchived);
    }

    @PostMapping("/api/v1/workspaces/{workspaceId}/boards")
    public ResponseEntity<BoardResponse> create(Principal principal, @PathVariable Long workspaceId,
                                                @Valid @RequestBody CreateBoardRequest request) {
        BoardResponse board = boardService.create(principal.getName(), workspaceId, request);
        return ResponseEntity.created(URI.create("/api/v1/boards/" + board.id())).body(board);
    }

    @GetMapping("/api/v1/boards/{boardId}")
    public BoardResponse get(Principal principal, @PathVariable Long boardId) {
        return boardService.get(principal.getName(), boardId);
    }

    @PutMapping("/api/v1/boards/{boardId}")
    public BoardResponse update(Principal principal, @PathVariable Long boardId,
                                @Valid @RequestBody UpdateBoardRequest request) {
        return boardService.update(principal.getName(), boardId, request);
    }

    @DeleteMapping("/api/v1/boards/{boardId}")
    public ResponseEntity<Void> delete(Principal principal, @PathVariable Long boardId) {
        boardService.delete(principal.getName(), boardId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/api/v1/boards/{boardId}/archive")
    public BoardSummaryResponse archive(Principal principal, @PathVariable Long boardId) {
        return boardService.archive(principal.getName(), boardId);
    }

    @PostMapping("/api/v1/boards/{boardId}/lists")
    public ResponseEntity<BoardListDetailsResponse> createList(Principal principal, @PathVariable Long boardId,
                                                               @Valid @RequestBody CreateBoardListRequest request) {
        BoardListDetailsResponse list = boardService.createList(principal.getName(), boardId, request);
        return ResponseEntity.created(URI.create("/api/v1/lists/" + list.id())).body(list);
    }
}
