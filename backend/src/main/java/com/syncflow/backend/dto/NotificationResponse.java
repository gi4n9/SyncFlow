package com.syncflow.backend.dto;

import java.time.Instant;
import java.util.Map;

public record NotificationResponse(Long id, String type, String message,
                                   Map<String, Object> payload, boolean read, Instant createdAt) {}
