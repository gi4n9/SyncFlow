package com.syncflow.backend.controller;

import com.syncflow.backend.dto.NotificationResponse;
import com.syncflow.backend.dto.PageResponse;
import com.syncflow.backend.service.NotificationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/notifications")
public class NotificationController {
    private final NotificationService notificationService;
    public NotificationController(NotificationService notificationService) { this.notificationService = notificationService; }

    @GetMapping
    public PageResponse<NotificationResponse> list(Principal principal,
                                                    @RequestParam(defaultValue = "0") int page,
                                                    @RequestParam(defaultValue = "20") int size) {
        return notificationService.list(principal.getName(), page, size);
    }

    @GetMapping("/unread-count")
    public Map<String, Long> unreadCount(Principal principal) {
        return Map.of("count", notificationService.unreadCount(principal.getName()));
    }

    @PatchMapping("/{notificationId}/read")
    public NotificationResponse read(Principal principal, @PathVariable Long notificationId) {
        return notificationService.markRead(principal.getName(), notificationId);
    }

    @PatchMapping("/read-all")
    public ResponseEntity<Void> readAll(Principal principal) {
        notificationService.markAllRead(principal.getName());
        return ResponseEntity.noContent().build();
    }
}
