package com.syncflow.backend.dto;

import com.syncflow.backend.model.WorkspaceRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class InviteMemberRequest {
    @NotBlank @Email @Size(max = 255)
    private String email;
    @NotNull
    private WorkspaceRole role;

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public WorkspaceRole getRole() { return role; }
    public void setRole(WorkspaceRole role) { this.role = role; }
}
