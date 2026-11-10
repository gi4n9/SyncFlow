package com.syncflow.backend.dto;

public record TokenSession(AuthResponse response, String refreshToken) {}
