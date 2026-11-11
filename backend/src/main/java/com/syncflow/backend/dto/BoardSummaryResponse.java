package com.syncflow.backend.dto;

import java.time.LocalDateTime;

public record BoardSummaryResponse(Long id, Long workspaceId, String name,
                                   int position, boolean archived, LocalDateTime createdAt) {}
