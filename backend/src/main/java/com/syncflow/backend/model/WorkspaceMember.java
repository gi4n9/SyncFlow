package com.syncflow.backend.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "workspace_members", uniqueConstraints =
    @UniqueConstraint(name = "uk_workspace_member", columnNames = {"workspace_id", "user_id"}))
public class WorkspaceMember {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "workspace_id", nullable = false)
    private Workspace workspace;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private WorkspaceRole role;

    @Column(name = "created_at")
    private LocalDateTime joinedAt = LocalDateTime.now();

    protected WorkspaceMember() {}

    public WorkspaceMember(Workspace workspace, User user, WorkspaceRole role) {
        this.workspace = workspace;
        this.user = user;
        this.role = role;
    }

    public Long getId() { return id; }
    public Workspace getWorkspace() { return workspace; }
    public User getUser() { return user; }
    public WorkspaceRole getRole() { return role; }
    public void setRole(WorkspaceRole role) { this.role = role; }
    public LocalDateTime getJoinedAt() { return joinedAt; }
}
