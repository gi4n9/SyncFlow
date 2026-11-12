package com.syncflow.backend.controller;

import com.syncflow.backend.dto.CommentResponse;
import com.syncflow.backend.dto.CreateCommentRequest;
import com.syncflow.backend.dto.PageResponse;
import com.syncflow.backend.dto.UpdateCommentRequest;
import com.syncflow.backend.service.CommentService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.security.Principal;

@RestController
public class CommentController {
    private final CommentService commentService;

    public CommentController(CommentService commentService) { this.commentService = commentService; }

    @GetMapping("/api/v1/cards/{cardId}/comments")
    public PageResponse<CommentResponse> list(Principal principal, @PathVariable Long cardId,
                                               @RequestParam(defaultValue = "0") int page,
                                               @RequestParam(defaultValue = "20") int size) {
        return commentService.list(principal.getName(), cardId, page, size);
    }

    @PostMapping("/api/v1/cards/{cardId}/comments")
    public ResponseEntity<CommentResponse> create(Principal principal, @PathVariable Long cardId,
                                                   @Valid @RequestBody CreateCommentRequest request) {
        CommentResponse response = commentService.create(principal.getName(), cardId, request);
        return ResponseEntity.created(URI.create("/api/v1/comments/" + response.id())).body(response);
    }

    @PutMapping("/api/v1/comments/{commentId}")
    public CommentResponse update(Principal principal, @PathVariable Long commentId,
                                  @Valid @RequestBody UpdateCommentRequest request) {
        return commentService.update(principal.getName(), commentId, request);
    }

    @DeleteMapping("/api/v1/comments/{commentId}")
    public ResponseEntity<Void> delete(Principal principal, @PathVariable Long commentId) {
        commentService.delete(principal.getName(), commentId);
        return ResponseEntity.noContent().build();
    }
}
