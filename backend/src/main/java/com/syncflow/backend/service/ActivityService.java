package com.syncflow.backend.service;

import com.syncflow.backend.dto.ActivityResponse;
import com.syncflow.backend.dto.PageResponse;
import com.syncflow.backend.model.ActivityLog;
import com.syncflow.backend.repository.ActivityLogRepository;
import com.syncflow.backend.repository.BoardRepository;
import com.syncflow.backend.repository.CardRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ActivityService {
    private final ActivityLogRepository activityRepository;
    private final BoardRepository boardRepository;
    private final CardRepository cardRepository;
    private final WorkspacePermissionService permissionService;

    public ActivityService(ActivityLogRepository activityRepository, BoardRepository boardRepository,
                           CardRepository cardRepository, WorkspacePermissionService permissionService) {
        this.activityRepository = activityRepository;
        this.boardRepository = boardRepository;
        this.cardRepository = cardRepository;
        this.permissionService = permissionService;
    }

    @Transactional(readOnly = true)
    public PageResponse<ActivityResponse> boardTimeline(String email, Long boardId, int page, int size) {
        var board = boardRepository.findWithWorkspace(boardId).orElseThrow(BoardService.BoardNotFoundException::new);
        permissionService.requireMember(email, board.getWorkspace().getId());
        var result = activityRepository.findAllByBoard_IdOrderByCreatedAtDescIdDesc(boardId, pageRequest(page, size));
        return new PageResponse<>(result.getContent().stream().map(this::response).toList(), page,
            result.getSize(), result.getTotalElements(), result.hasNext());
    }

    @Transactional(readOnly = true)
    public PageResponse<ActivityResponse> cardTimeline(String email, Long cardId, int page, int size) {
        var card = cardRepository.findWithContextById(cardId).orElseThrow(CardService.CardNotFoundException::new);
        permissionService.requireMember(email, card.getBoardList().getBoard().getWorkspace().getId());
        var result = activityRepository.findAllByCard_IdOrderByCreatedAtDescIdDesc(cardId, pageRequest(page, size));
        return new PageResponse<>(result.getContent().stream().map(this::response).toList(), page,
            result.getSize(), result.getTotalElements(), result.hasNext());
    }

    private PageRequest pageRequest(int page, int size) {
        if (page < 0) throw new IllegalArgumentException("page must be zero or greater");
        if (size < 1 || size > 100) throw new IllegalArgumentException("size must be between 1 and 100");
        return PageRequest.of(page, size);
    }

    private ActivityResponse response(ActivityLog entry) {
        return new ActivityResponse(entry.getId(), entry.getWorkspace().getId(),
            entry.getBoard() == null ? null : entry.getBoard().getId(),
            entry.getCard() == null ? null : entry.getCard().getId(),
            entry.getActor() == null ? null : entry.getActor().getId(),
            entry.getActor() == null ? "Deleted user" : entry.getActor().getFullName(),
            entry.getAction(), entry.getMetadata(), entry.getCreatedAt());
    }
}
