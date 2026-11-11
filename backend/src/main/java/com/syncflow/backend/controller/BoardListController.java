package com.syncflow.backend.controller;

import com.syncflow.backend.dto.BoardListDetailsResponse;
import com.syncflow.backend.dto.MoveBoardListRequest;
import com.syncflow.backend.dto.UpdateBoardListRequest;
import com.syncflow.backend.service.BoardService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@RestController
@RequestMapping("/api/v1/lists")
public class BoardListController {
    private final BoardService boardService;

    public BoardListController(BoardService boardService) {
        this.boardService = boardService;
    }

    @PutMapping("/{listId}")
    public BoardListDetailsResponse update(Principal principal, @PathVariable Long listId,
                                           @Valid @RequestBody UpdateBoardListRequest request) {
        return boardService.updateList(principal.getName(), listId, request);
    }

    @PatchMapping("/{listId}/move")
    public BoardListDetailsResponse move(Principal principal, @PathVariable Long listId,
                                         @Valid @RequestBody MoveBoardListRequest request) {
        return boardService.moveList(principal.getName(), listId, request);
    }

    @DeleteMapping("/{listId}")
    public ResponseEntity<Void> delete(Principal principal, @PathVariable Long listId) {
        boardService.deleteList(principal.getName(), listId);
        return ResponseEntity.noContent().build();
    }
}
