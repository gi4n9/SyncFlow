package com.syncflow.backend.controller;

import com.syncflow.backend.dto.ActivityResponse;
import com.syncflow.backend.dto.PageResponse;
import com.syncflow.backend.service.ActivityService;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@RestController
public class ActivityController {
    private final ActivityService activityService;
    public ActivityController(ActivityService activityService) { this.activityService = activityService; }

    @GetMapping("/api/v1/boards/{boardId}/activities")
    public PageResponse<ActivityResponse> board(Principal principal, @PathVariable Long boardId,
                                                 @RequestParam(defaultValue = "0") int page,
                                                 @RequestParam(defaultValue = "20") int size) {
        return activityService.boardTimeline(principal.getName(), boardId, page, size);
    }

    @GetMapping("/api/v1/cards/{cardId}/activities")
    public PageResponse<ActivityResponse> card(Principal principal, @PathVariable Long cardId,
                                                @RequestParam(defaultValue = "0") int page,
                                                @RequestParam(defaultValue = "20") int size) {
        return activityService.cardTimeline(principal.getName(), cardId, page, size);
    }
}
