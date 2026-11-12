package com.syncflow.backend.controller;

import com.syncflow.backend.dto.CardLabelResponse;
import com.syncflow.backend.service.LabelService;
import org.springframework.web.bind.annotation.*;
import java.security.Principal;
import java.util.List;

@RestController
public class LabelController {
    private final LabelService labelService;
    public LabelController(LabelService labelService) { this.labelService = labelService; }

    @GetMapping("/api/v1/boards/{boardId}/labels")
    public List<CardLabelResponse> list(Principal principal, @PathVariable Long boardId) {
        return labelService.list(principal.getName(), boardId);
    }

    @GetMapping("/api/v1/workspaces/{workspaceId}/labels")
    public List<CardLabelResponse> listForWorkspace(Principal principal, @PathVariable Long workspaceId) {
        return labelService.listForWorkspace(principal.getName(), workspaceId);
    }
}
