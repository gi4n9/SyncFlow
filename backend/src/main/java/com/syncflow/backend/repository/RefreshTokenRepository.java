package com.syncflow.backend.repository;

import com.syncflow.backend.model.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Lock;
import java.util.Optional;
import java.time.Instant;
import java.util.UUID;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    @EntityGraph(attributePaths = "user")
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<RefreshToken> findByTokenHash(String tokenHash);

    @Modifying(clearAutomatically = true)
    @Query("update RefreshToken token set token.revokedAt = :now where token.familyId = :familyId and token.revokedAt is null")
    int revokeFamily(UUID familyId, Instant now);

    @Modifying(clearAutomatically = true)
    @Query("update RefreshToken token set token.revokedAt = :now where token.user.id = :userId and token.revokedAt is null")
    int revokeAllForUser(Long userId, Instant now);
}
