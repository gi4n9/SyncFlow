package com.syncflow.backend.event;

import java.util.List;
import java.util.Map;

public record ActivityEvent(Long actorId, Long workspaceId, Long boardId, Long cardId,
                            String action, Map<String, Object> metadata,
                            List<NotificationTarget> notifications) {
    public ActivityEvent {
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
        notifications = notifications == null ? List.of() : List.copyOf(notifications);
    }
}
