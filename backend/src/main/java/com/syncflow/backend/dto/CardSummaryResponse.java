package com.syncflow.backend.dto;

import java.time.Instant;
import com.syncflow.backend.model.CardPriority;
import java.util.List;

public record CardSummaryResponse(Long id, String title, String description,
                                  double position, Instant dueDate, CardPriority priority,
                                  long version, Long createdById, List<CardAssigneeResponse> assignees) {}
