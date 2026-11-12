package com.syncflow.backend.dto;

import java.time.Instant;
import java.util.Map;

public record BoardRealtimeEvent(Long id, Long boardId, Long cardId, Long actorId,
                                 String actorName, String action,
                                 Map<String, Object> metadata, Instant createdAt) {}
