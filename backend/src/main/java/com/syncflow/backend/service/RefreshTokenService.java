package com.syncflow.backend.service;

import com.syncflow.backend.model.RefreshToken;
import com.syncflow.backend.model.User;
import com.syncflow.backend.repository.RefreshTokenRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;

@Service
public class RefreshTokenService {
    private final RefreshTokenRepository repository;
    private final long expirationMillis;
    private final SecureRandom secureRandom = new SecureRandom();

    public RefreshTokenService(RefreshTokenRepository repository,
                               @Value("${jwt.refresh-expiration:604800000}") long expirationMillis) {
        if (expirationMillis <= 0) throw new IllegalArgumentException("Refresh token expiration must be positive");
        this.repository = repository;
        this.expirationMillis = expirationMillis;
    }

    @Transactional
    public String create(User user) {
        byte[] bytes = new byte[64];
        secureRandom.nextBytes(bytes);
        String rawToken = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        repository.save(new RefreshToken(user, hash(rawToken), Instant.now().plusMillis(expirationMillis)));
        return rawToken;
    }

    @Transactional
    public User rotate(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) throw new InvalidRefreshTokenException();
        RefreshToken stored = repository.findByTokenHash(hash(rawToken))
            .orElseThrow(InvalidRefreshTokenException::new);
        if (!stored.getExpiresAt().isAfter(Instant.now())) {
            repository.delete(stored);
            throw new InvalidRefreshTokenException();
        }
        User user = stored.getUser();
        repository.delete(stored);
        repository.flush();
        return user;
    }

    private String hash(String token) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
            return java.util.HexFormat.of().formatHex(digest);
        } catch (Exception exception) {
            throw new IllegalStateException("Could not hash refresh token", exception);
        }
    }

    public static class InvalidRefreshTokenException extends RuntimeException {
        public InvalidRefreshTokenException() { super("Invalid or expired refresh token"); }
    }
}
