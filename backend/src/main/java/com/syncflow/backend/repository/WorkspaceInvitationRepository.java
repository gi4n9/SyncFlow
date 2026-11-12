package com.syncflow.backend.repository;

import com.syncflow.backend.model.InvitationStatus;
import com.syncflow.backend.model.WorkspaceInvitation;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface WorkspaceInvitationRepository extends JpaRepository<WorkspaceInvitation, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = {"workspace", "invitedBy"})
    Optional<WorkspaceInvitation> findByTokenHash(String tokenHash);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select invitation from WorkspaceInvitation invitation join fetch invitation.workspace join fetch invitation.invitedBy where invitation.id = :id")
    Optional<WorkspaceInvitation> findLockedById(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<WorkspaceInvitation> findByWorkspace_IdAndEmailAndStatus(
        Long workspaceId, String email, InvitationStatus status);

    boolean existsByWorkspace_IdAndEmailAndStatus(Long workspaceId, String email, InvitationStatus status);
}
