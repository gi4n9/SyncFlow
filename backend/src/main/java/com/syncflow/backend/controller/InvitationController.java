package com.syncflow.backend.controller;

import com.syncflow.backend.dto.InvitationAcceptedResponse;
import com.syncflow.backend.service.WorkspaceService;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;

import java.security.Principal;

@RestController
@RequestMapping("/api/v1")
public class InvitationController {
    private final WorkspaceService workspaceService;

    public InvitationController(WorkspaceService workspaceService) {
        this.workspaceService = workspaceService;
    }

    @PostMapping("/invitations/{token}/accept")
    public InvitationAcceptedResponse accept(Principal principal, @PathVariable String token) {
        return workspaceService.acceptInvitation(principal.getName(), token);
    }

    @PostMapping("/my/invitations/{invitationId}/accept")
    public InvitationAcceptedResponse acceptPending(Principal principal, @PathVariable Long invitationId) {
        return workspaceService.acceptInvitationById(principal.getName(), invitationId);
    }

    @PostMapping("/my/invitations/{invitationId}/decline")
    public ResponseEntity<Void> decline(Principal principal, @PathVariable Long invitationId) {
        workspaceService.declineInvitation(principal.getName(), invitationId);
        return ResponseEntity.noContent().build();
    }
}
