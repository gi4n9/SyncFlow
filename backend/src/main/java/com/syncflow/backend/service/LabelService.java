package com.syncflow.backend.service;

import com.syncflow.backend.dto.CardLabelResponse;
import com.syncflow.backend.repository.BoardRepository;
import com.syncflow.backend.repository.LabelRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
public class LabelService {
    private final BoardRepository boardRepository;
    private final LabelRepository labelRepository;
    private final WorkspacePermissionService permissionService;

    public LabelService(BoardRepository boardRepository, LabelRepository labelRepository,
                        WorkspacePermissionService permissionService) {
        this.boardRepository = boardRepository;
        this.labelRepository = labelRepository;
        this.permissionService = permissionService;
    }

    @Transactional(readOnly = true)
    public List<CardLabelResponse> list(String email, Long boardId) {
        var board = boardRepository.findWithWorkspace(boardId).orElseThrow(BoardService.BoardNotFoundException::new);
        permissionService.requireMember(email, board.getWorkspace().getId());
        return labelRepository.findAllByBoard_IdOrderByNameAsc(boardId).stream()
            .map(label -> new CardLabelResponse(label.getId(), label.getName(), label.getColor())).toList();
    }

    @Transactional(readOnly = true)
    public List<CardLabelResponse> listForWorkspace(String email, Long workspaceId) {
        permissionService.requireMember(email, workspaceId);
        var unique = new java.util.LinkedHashMap<String, CardLabelResponse>();
        labelRepository.findAllByBoard_Workspace_IdOrderByNameAsc(workspaceId).forEach(label ->
            unique.putIfAbsent(label.getName().toLowerCase(java.util.Locale.ROOT),
                new CardLabelResponse(label.getId(), label.getName(), label.getColor())));
        return List.copyOf(unique.values());
    }
}
