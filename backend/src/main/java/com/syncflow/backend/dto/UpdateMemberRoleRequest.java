package com.syncflow.backend.dto;

import com.syncflow.backend.model.WorkspaceRole;
import jakarta.validation.constraints.NotNull;

public class UpdateMemberRoleRequest {
    @NotNull
    private WorkspaceRole role;

    public WorkspaceRole getRole() { return role; }
    public void setRole(WorkspaceRole role) { this.role = role; }
}
