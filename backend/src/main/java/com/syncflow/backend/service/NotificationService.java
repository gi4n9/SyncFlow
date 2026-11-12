package com.syncflow.backend.service;

import com.syncflow.backend.dto.NotificationResponse;
import com.syncflow.backend.dto.PageResponse;
import com.syncflow.backend.model.Notification;
import com.syncflow.backend.repository.NotificationRepository;
import com.syncflow.backend.repository.UserRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationService {
    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    public NotificationService(NotificationRepository notificationRepository, UserRepository userRepository) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public PageResponse<NotificationResponse> list(String email, int page, int size) {
        Long userId = userId(email);
        PageRequest request = pageRequest(page, size);
        var result = notificationRepository.findAllByUser_IdOrderByCreatedAtDescIdDesc(userId, request);
        return new PageResponse<>(result.getContent().stream().map(this::response).toList(), page,
            result.getSize(), result.getTotalElements(), result.hasNext());
    }

    @Transactional(readOnly = true)
    public long unreadCount(String email) {
        return notificationRepository.countByUser_IdAndReadFalse(userId(email));
    }

    @Transactional
    public NotificationResponse markRead(String email, Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
            .orElseThrow(NotificationNotFoundException::new);
        if (!notification.getUser().getId().equals(userId(email))) throw new AccessDeniedException("Notification belongs to another user");
        notification.markRead();
        return response(notificationRepository.save(notification));
    }

    @Transactional
    public int markAllRead(String email) {
        return notificationRepository.markAllRead(userId(email));
    }

    private Long userId(String email) {
        return userRepository.findByEmail(email).orElseThrow(AuthService.UserNotFoundException::new).getId();
    }

    private PageRequest pageRequest(int page, int size) {
        if (page < 0) throw new IllegalArgumentException("page must be zero or greater");
        if (size < 1 || size > 100) throw new IllegalArgumentException("size must be between 1 and 100");
        return PageRequest.of(page, size);
    }

    private NotificationResponse response(Notification notification) {
        return new NotificationResponse(notification.getId(), notification.getType(), notification.getMessage(),
            notification.getPayload(), notification.isRead(), notification.getCreatedAt());
    }

    public static class NotificationNotFoundException extends RuntimeException {
        public NotificationNotFoundException() { super("Notification not found"); }
    }
}
