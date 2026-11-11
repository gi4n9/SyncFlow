package com.syncflow.backend.controller;

import com.syncflow.backend.dto.*;
import com.syncflow.backend.service.WorkspaceService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/v1/workspaces")
public class WorkspaceController {
    private final WorkspaceService workspaceService;

    public WorkspaceController(WorkspaceService workspaceService) {
        this.workspaceService = workspaceService;
    }

    @PostMapping
    public ResponseEntity<WorkspaceResponse> create(Principal principal,
                                                     @Valid @RequestBody CreateWorkspaceRequest request) {
        WorkspaceResponse created = workspaceService.create(principal.getName(), request);
        return ResponseEntity.created(URI.create("/api/v1/workspaces/" + created.id())).body(created);
    }

    @GetMapping
    public List<WorkspaceResponse> listMine(Principal principal) {
        return workspaceService.listMine(principal.getName());
    }

    @GetMapping("/{workspaceId}")
    public WorkspaceResponse get(Principal principal, @PathVariable Long workspaceId) {
        return workspaceService.get(principal.getName(), workspaceId);
    }

    @PutMapping("/{workspaceId}")
    public WorkspaceResponse update(Principal principal, @PathVariable Long workspaceId,
                                    @Valid @RequestBody UpdateWorkspaceRequest request) {
        return workspaceService.update(principal.getName(), workspaceId, request);
    }

    @DeleteMapping("/{workspaceId}")
    public ResponseEntity<Void> delete(Principal principal, @PathVariable Long workspaceId) {
        workspaceService.delete(principal.getName(), workspaceId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{workspaceId}/members")
    public List<WorkspaceMemberResponse> members(Principal principal, @PathVariable Long workspaceId) {
        return workspaceService.listMembers(principal.getName(), workspaceId);
    }

    @PostMapping("/{workspaceId}/invitations")
    public ResponseEntity<InvitationResponse> invite(Principal principal, @PathVariable Long workspaceId,
                                                     @Valid @RequestBody InviteMemberRequest request) {
        return ResponseEntity.status(201).body(workspaceService.invite(principal.getName(), workspaceId, request));
    }

    @PatchMapping("/{workspaceId}/members/{userId}")
    public WorkspaceMemberResponse updateRole(Principal principal, @PathVariable Long workspaceId,
                                               @PathVariable Long userId,
                                               @Valid @RequestBody UpdateMemberRoleRequest request) {
        return workspaceService.updateMemberRole(principal.getName(), workspaceId, userId, request);
    }

    @DeleteMapping("/{workspaceId}/members/{userId}")
    public ResponseEntity<Void> removeMember(Principal principal, @PathVariable Long workspaceId,
                                             @PathVariable Long userId) {
        workspaceService.removeMember(principal.getName(), workspaceId, userId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{workspaceId}/members/me")
    public ResponseEntity<Void> leave(Principal principal, @PathVariable Long workspaceId) {
        workspaceService.leave(principal.getName(), workspaceId);
        return ResponseEntity.noContent().build();
    }
}
