package com.syncflow.backend.service;

import com.syncflow.backend.dto.NotificationResponse;
import com.syncflow.backend.dto.BoardRealtimeEvent;
import com.syncflow.backend.event.ActivityEvent;
import com.syncflow.backend.event.NotificationTarget;
import com.syncflow.backend.model.ActivityLog;
import com.syncflow.backend.model.Notification;
import com.syncflow.backend.repository.*;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.ArrayList;
import java.util.List;

@Component
public class ActivityEventListener {
    private final ActivityLogRepository activityRepository;
    private final NotificationRepository notificationRepository;
    private final WorkspaceRepository workspaceRepository;
    private final BoardRepository boardRepository;
    private final CardRepository cardRepository;
    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;

    public ActivityEventListener(ActivityLogRepository activityRepository,
                                 NotificationRepository notificationRepository,
                                 WorkspaceRepository workspaceRepository,
                                 BoardRepository boardRepository,
                                 CardRepository cardRepository,
                                 UserRepository userRepository,
                                 SimpMessagingTemplate messagingTemplate) {
        this.activityRepository = activityRepository;
        this.notificationRepository = notificationRepository;
        this.workspaceRepository = workspaceRepository;
        this.boardRepository = boardRepository;
        this.cardRepository = cardRepository;
        this.userRepository = userRepository;
        this.messagingTemplate = messagingTemplate;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onActivity(ActivityEvent event) {
        var workspace = workspaceRepository.findById(event.workspaceId()).orElse(null);
        if (workspace == null) return;

        var board = event.boardId() == null ? null : boardRepository.findById(event.boardId()).orElse(null);
        var card = event.cardId() == null ? null : cardRepository.findById(event.cardId()).orElse(null);
        var actor = event.actorId() == null ? null : userRepository.findById(event.actorId()).orElse(null);
        ActivityLog activity = activityRepository.saveAndFlush(new ActivityLog(workspace, board, card, actor,
            event.action(), event.metadata()));

        List<PendingPush> pushes = new ArrayList<>();
        for (NotificationTarget target : event.notifications()) {
            var user = userRepository.findById(target.userId()).orElse(null);
            if (user == null) continue;
            Notification notification = notificationRepository.save(new Notification(
                user, target.type(), target.message(), target.payload()));
            pushes.add(new PendingPush(user.getEmail(), new NotificationResponse(notification.getId(),
                notification.getType(), notification.getMessage(), notification.getPayload(),
                notification.isRead(), notification.getCreatedAt())));
        }
        notificationRepository.flush();

        if ((!pushes.isEmpty() || event.boardId() != null) && TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    if (event.boardId() != null) {
                        messagingTemplate.convertAndSend("/topic/boards/" + event.boardId(),
                            new BoardRealtimeEvent(activity.getId(), event.boardId(), event.cardId(),
                                event.actorId(), actor == null ? null : actor.getFullName(), event.action(),
                                event.metadata(), activity.getCreatedAt()));
                    }
                    for (PendingPush push : pushes) {
                        messagingTemplate.convertAndSendToUser(push.email(), "/queue/notifications", push.notification());
                    }
                }
            });
        }
    }

    private record PendingPush(String email, NotificationResponse notification) {}
}
