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
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class RefreshTokenService {
    private static final Logger logger = LoggerFactory.getLogger(RefreshTokenService.class);
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
        UUID familyId = UUID.randomUUID();
        String raw = generateToken();
        repository.save(new RefreshToken(user, hash(raw), Instant.now().plusMillis(expirationMillis), familyId));
        return raw;
    }

    @Transactional(noRollbackFor = ReuseDetectedException.class)
    public RotatedToken rotate(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) throw new InvalidRefreshTokenException();
        RefreshToken stored = repository.findByTokenHash(hash(rawToken))
            .orElseThrow(InvalidRefreshTokenException::new);
        Instant now = Instant.now();
        if (stored.getRevokedAt() != null) {
            repository.revokeFamily(stored.getFamilyId(), now);
            logger.warn("Refresh token reuse detected for family {}", stored.getFamilyId());
            throw new ReuseDetectedException();
        }
        if (!stored.getExpiresAt().isAfter(now)) {
            stored.revoke(now);
            throw new InvalidRefreshTokenException();
        }

        stored.revoke(now);
        String replacement = generateToken();
        repository.save(new RefreshToken(stored.getUser(), hash(replacement),
            now.plusMillis(expirationMillis), stored.getFamilyId()));
        return new RotatedToken(stored.getUser(), replacement);
    }

    @Transactional
    public void revoke(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) return;
        repository.findByTokenHash(hash(rawToken)).ifPresent(token -> {
            if (token.getRevokedAt() == null) token.revoke(Instant.now());
        });
    }

    @Transactional
    public void revokeAllForUser(Long userId) {
        repository.revokeAllForUser(userId, Instant.now());
    }

    private String generateToken() {
        byte[] bytes = new byte[64];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hash(String token) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
            return java.util.HexFormat.of().formatHex(digest);
        } catch (Exception exception) {
            throw new IllegalStateException("Could not hash refresh token", exception);
        }
    }

    public record RotatedToken(User user, String refreshToken) {}

    public static class InvalidRefreshTokenException extends RuntimeException {
        public InvalidRefreshTokenException() { super("Invalid or expired refresh token"); }
    }

    public static class ReuseDetectedException extends InvalidRefreshTokenException {
        public ReuseDetectedException() { super(); }
    }
}
