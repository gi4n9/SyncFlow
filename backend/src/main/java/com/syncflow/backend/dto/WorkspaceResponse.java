package com.syncflow.backend.dto;

public record WorkspaceResponse(Long id, String name, String description, Long ownerId, String role) {}
