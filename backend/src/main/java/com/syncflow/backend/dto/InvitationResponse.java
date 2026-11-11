package com.syncflow.backend.dto;

import com.syncflow.backend.model.WorkspaceRole;
import java.time.Instant;

public record InvitationResponse(Long workspaceId, String email, WorkspaceRole role,
                                 String invitationToken, Instant expiresAt) {}
