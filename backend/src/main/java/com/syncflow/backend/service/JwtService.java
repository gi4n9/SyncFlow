package com.syncflow.backend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.syncflow.backend.model.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;

@Service
public class JwtService {
    private static final Base64.Encoder ENCODER = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder DECODER = Base64.getUrlDecoder();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final byte[] secret;
    private final long expirationMillis;

    public JwtService(@Value("${jwt.secret}") String secret,
                      @Value("${jwt.expiration:86400000}") long expirationMillis) {
        this.secret = secret.getBytes(StandardCharsets.UTF_8);
        this.expirationMillis = expirationMillis;
        if (this.secret.length < 32) {
            throw new IllegalArgumentException("JWT secret must contain at least 32 bytes");
        }
        if (expirationMillis <= 0) throw new IllegalArgumentException("JWT expiration must be positive");
    }

    public String createAccessToken(User user) {
        try {
            Instant now = Instant.now();
            String header = encode(objectMapper.writeValueAsBytes(Map.of("alg", "HS256", "typ", "JWT")));
            String payload = encode(objectMapper.writeValueAsBytes(Map.of(
                "sub", user.getId().toString(),
                "iat", now.getEpochSecond(),
                "exp", now.plusMillis(expirationMillis).getEpochSecond(),
                "jti", UUID.randomUUID().toString()
            )));
            String content = header + "." + payload;
            return content + "." + ENCODER.encodeToString(sign(content));
        } catch (Exception exception) {
            throw new IllegalStateException("Could not create access token", exception);
        }
    }

    public Long getUserId(String token) {
        try {
            String[] parts = token.split("\\.", -1);
            if (parts.length != 3) throw new IllegalArgumentException("Malformed token");
            if (!java.security.MessageDigest.isEqual(sign(parts[0] + "." + parts[1]), DECODER.decode(parts[2]))) {
                throw new IllegalArgumentException("Invalid token signature");
            }
            var header = objectMapper.readTree(DECODER.decode(parts[0]));
            if (!"HS256".equals(header.path("alg").asText())) throw new IllegalArgumentException("Unsupported token algorithm");
            var claims = objectMapper.readTree(DECODER.decode(parts[1]));
            if (claims.path("exp").asLong(0) <= Instant.now().getEpochSecond()) {
                throw new IllegalArgumentException("Expired token");
            }
            return Long.valueOf(claims.path("sub").asText());
        } catch (Exception exception) {
            throw new IllegalArgumentException("Invalid or expired access token", exception);
        }
    }

    public long getExpirationMillis() { return expirationMillis; }

    private byte[] sign(String content) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret, "HmacSHA256"));
        return mac.doFinal(content.getBytes(StandardCharsets.US_ASCII));
    }

    private String encode(byte[] value) { return ENCODER.encodeToString(value); }
}
