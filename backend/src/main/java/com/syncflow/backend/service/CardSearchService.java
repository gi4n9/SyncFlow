package com.syncflow.backend.service;

import com.syncflow.backend.dto.*;
import com.syncflow.backend.model.Card;
import com.syncflow.backend.model.CardPriority;
import com.syncflow.backend.repository.BoardRepository;
import com.syncflow.backend.repository.CardRepository;
import com.syncflow.backend.repository.WorkspaceRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class CardSearchService {
    private final CardRepository cardRepository;
    private final BoardRepository boardRepository;
    private final WorkspaceRepository workspaceRepository;
    private final WorkspacePermissionService permissionService;

    public CardSearchService(CardRepository cardRepository, BoardRepository boardRepository,
                             WorkspaceRepository workspaceRepository, WorkspacePermissionService permissionService) {
        this.cardRepository = cardRepository;
        this.boardRepository = boardRepository;
        this.workspaceRepository = workspaceRepository;
        this.permissionService = permissionService;
    }

    @Transactional(readOnly = true)
    public PageResponse<SearchCardResponse> search(String email, Long boardId, Long workspaceId, String query,
                                                   Long assigneeId, String label, Instant dueFrom, Instant dueTo,
                                                   CardPriority priority, int page, int size) {
        if (boardId == null && workspaceId == null) throw new IllegalArgumentException("boardId or workspaceId is required");
        if (page < 0) throw new IllegalArgumentException("page must be zero or greater");
        if (size < 1 || size > 100) throw new IllegalArgumentException("size must be between 1 and 100");
        if (dueFrom != null && dueTo != null && !dueFrom.isBefore(dueTo)) throw new IllegalArgumentException("dueFrom must be earlier than dueTo");

        if (boardId != null) {
            var board = boardRepository.findWithWorkspace(boardId).orElseThrow(BoardService.BoardNotFoundException::new);
            permissionService.requireMember(email, board.getWorkspace().getId());
            if (workspaceId != null && !workspaceId.equals(board.getWorkspace().getId())) {
                throw new BoardService.BoardNotFoundException();
            }
            workspaceId = board.getWorkspace().getId();
        } else {
            permissionService.requireMember(email, workspaceId);
            if (!workspaceRepository.existsById(workspaceId)) throw new WorkspaceService.WorkspaceNotFoundException();
        }

        String cleanQuery = query == null || query.isBlank() ? null : query.trim();
        String cleanLabel = label == null || label.isBlank() ? null : label.trim();
        var result = cardRepository.searchCardIds(boardId, workspaceId, cleanQuery, assigneeId, cleanLabel,
            dueFrom, dueTo, priority == null ? null : priority.name(), PageRequest.of(page, size));
        if (result.isEmpty()) return new PageResponse<>(java.util.List.of(), page, size, result.getTotalElements(), false);
        Map<Long, Card> cards = cardRepository.findAllWithSearchDetailsByIdIn(result.getContent()).stream()
            .collect(Collectors.toMap(Card::getId, card -> card));
        var content = result.getContent().stream().map(cards::get).filter(java.util.Objects::nonNull)
            .map(this::response).toList();
        return new PageResponse<>(content, page, size, result.getTotalElements(), result.hasNext());
    }

    private SearchCardResponse response(Card card) {
        var list = card.getBoardList();
        var board = list.getBoard();
        var assignees = card.getAssignees().stream()
            .map(user -> new CardAssigneeResponse(user.getId(), user.getEmail(), user.getFullName())).toList();
        var labels = card.getLabels().stream()
            .map(label -> new CardLabelResponse(label.getId(), label.getName(), label.getColor())).toList();
        return new SearchCardResponse(card.getId(), card.getTitle(), card.getDescription(), card.getDueDate(),
            card.getPriority(), card.getVersion(), board.getWorkspace().getId(), board.getId(), board.getTitle(),
            list.getId(), list.getName(), assignees, labels);
    }
}
