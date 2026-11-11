package com.syncflow.backend.dto;

import java.time.LocalDateTime;
import java.util.List;

public record BoardResponse(Long id, Long workspaceId, String name, boolean archived,
                            LocalDateTime createdAt, List<BoardListResponse> lists) {}
