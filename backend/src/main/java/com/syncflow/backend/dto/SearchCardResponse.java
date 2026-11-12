package com.syncflow.backend.dto;

import com.syncflow.backend.model.CardPriority;
import java.time.Instant;
import java.util.List;

public record SearchCardResponse(Long id, String title, String description, Instant dueDate,
                                 CardPriority priority, long version, Long workspaceId, Long boardId, String boardName,
                                 Long listId, String listName, List<CardAssigneeResponse> assignees,
                                 List<CardLabelResponse> labels) {}
