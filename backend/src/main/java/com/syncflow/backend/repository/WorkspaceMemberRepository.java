package com.syncflow.backend.repository;

import com.syncflow.backend.model.WorkspaceMember;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface WorkspaceMemberRepository extends JpaRepository<WorkspaceMember, Long> {
    Optional<WorkspaceMember> findByWorkspace_IdAndUser_Email(Long workspaceId, String email);
    Optional<WorkspaceMember> findByWorkspace_IdAndUser_Id(Long workspaceId, Long userId);
    boolean existsByWorkspace_IdAndUser_Id(Long workspaceId, Long userId);
    List<WorkspaceMember> findAllByWorkspace_IdAndUser_IdIn(Long workspaceId, Set<Long> userIds);

    @EntityGraph(attributePaths = {"user", "workspace"})
    List<WorkspaceMember> findAllByWorkspace_IdOrderByJoinedAtAsc(Long workspaceId);

    @Query("select member from WorkspaceMember member join fetch member.workspace where member.user.email = :email order by member.joinedAt")
    List<WorkspaceMember> findAllForUser(@Param("email") String email);
}
