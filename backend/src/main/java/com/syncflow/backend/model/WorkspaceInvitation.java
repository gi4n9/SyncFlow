package com.syncflow.backend.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "workspace_invitations")
public class WorkspaceInvitation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "workspace_id", nullable = false)
    private Workspace workspace;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "invited_by", nullable = false)
    private User invitedBy;

    @Column(nullable = false, length = 255)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private WorkspaceRole role;

    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    private String tokenHash;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private InvitationStatus status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    protected WorkspaceInvitation() {}

    public WorkspaceInvitation(Workspace workspace, User invitedBy, String email, WorkspaceRole role,
                               String tokenHash, Instant expiresAt) {
        this.workspace = workspace;
        this.invitedBy = invitedBy;
        this.email = email;
        this.role = role;
        this.tokenHash = tokenHash;
        this.expiresAt = expiresAt;
        this.status = InvitationStatus.PENDING;
    }

    public Long getId() { return id; }
    public Workspace getWorkspace() { return workspace; }
    public User getInvitedBy() { return invitedBy; }
    public String getEmail() { return email; }
    public WorkspaceRole getRole() { return role; }
    public Instant getExpiresAt() { return expiresAt; }
    public InvitationStatus getStatus() { return status; }
    public void setStatus(InvitationStatus status) { this.status = status; }
}
