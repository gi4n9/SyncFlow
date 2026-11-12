package com.syncflow.backend.dto;

import java.time.Instant;

public record CommentResponse(Long id, Long cardId, Long authorId, String authorName,
                              String authorEmail, String content, Instant createdAt, Instant editedAt) {}
