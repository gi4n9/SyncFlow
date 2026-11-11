package com.syncflow.backend.dto;

import java.time.LocalDateTime;

public record WorkspaceMemberResponse(Long userId, String email, String fullName,
                                      String role, LocalDateTime joinedAt) {}
