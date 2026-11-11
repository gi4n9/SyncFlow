package com.syncflow.backend.dto;

import java.util.List;

public record BoardListResponse(Long id, String name, double position, List<CardSummaryResponse> cards) {}
