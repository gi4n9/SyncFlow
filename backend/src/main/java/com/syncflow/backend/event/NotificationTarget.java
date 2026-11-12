package com.syncflow.backend.event;

import java.util.Map;

public record NotificationTarget(Long userId, String type, String message, Map<String, Object> payload) {
    public NotificationTarget {
        payload = payload == null ? Map.of() : Map.copyOf(payload);
    }
}
