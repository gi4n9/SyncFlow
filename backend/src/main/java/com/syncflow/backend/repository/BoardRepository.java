package com.syncflow.backend.repository;

import com.syncflow.backend.model.Board;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface BoardRepository extends JpaRepository<Board, Long> {
    @EntityGraph(attributePaths = "workspace")
    List<Board> findAllByWorkspace_IdOrderByPositionAsc(Long workspaceId);

    @EntityGraph(attributePaths = "workspace")
    List<Board> findAllByWorkspace_IdAndArchivedFalseOrderByPositionAsc(Long workspaceId);

    @Query("select board from Board board join fetch board.workspace where board.id = :id")
    Optional<Board> findWithWorkspace(@Param("id") Long id);

    @Query("select coalesce(max(board.position), 0) from Board board where board.workspace.id = :workspaceId")
    Integer findMaxPosition(@Param("workspaceId") Long workspaceId);
}
