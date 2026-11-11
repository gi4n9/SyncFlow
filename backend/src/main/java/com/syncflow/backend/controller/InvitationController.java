package com.syncflow.backend.controller;

import com.syncflow.backend.dto.InvitationAcceptedResponse;
import com.syncflow.backend.service.WorkspaceService;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@RestController
@RequestMapping("/api/v1/invitations")
public class InvitationController {
    private final WorkspaceService workspaceService;

    public InvitationController(WorkspaceService workspaceService) {
        this.workspaceService = workspaceService;
    }

    @PostMapping("/{token}/accept")
    public InvitationAcceptedResponse accept(Principal principal, @PathVariable String token) {
        return workspaceService.acceptInvitation(principal.getName(), token);
    }
}
