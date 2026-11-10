package com.syncflow.backend.dto;

public class AuthResponse {
    private final Long id;
    private final String email;
    private final String fullName;
    private final String accessToken;
    private final String refreshToken;
    private final String tokenType = "Bearer";
    private final long expiresIn;

    public AuthResponse(Long id, String email, String fullName, String accessToken, String refreshToken, long expiresIn) {
        this.id = id;
        this.email = email;
        this.fullName = fullName;
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.expiresIn = expiresIn;
    }

    public Long getId() { return id; }
    public String getEmail() { return email; }
    public String getFullName() { return fullName; }
    public String getAccessToken() { return accessToken; }
    public String getRefreshToken() { return refreshToken; }
    public String getTokenType() { return tokenType; }
    public long getExpiresIn() { return expiresIn; }
}
