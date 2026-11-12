package com.syncflow.backend.dto;

import java.time.Instant;
import java.util.Map;

public record ActivityResponse(Long id, Long workspaceId, Long boardId, Long cardId,
                               Long actorId, String actorName, String action,
                               Map<String, Object> metadata, Instant createdAt) {}
