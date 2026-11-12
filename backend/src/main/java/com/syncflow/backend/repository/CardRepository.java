package com.syncflow.backend.repository;

import com.syncflow.backend.model.Card;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CardRepository extends JpaRepository<Card, Long> {
    @Query("select distinct card from Card card join fetch card.boardList boardList " +
        "left join fetch card.assignees left join fetch card.createdBy left join fetch card.labels " +
        "where boardList.board.id = :boardId order by card.position asc")
    List<Card> findAllByBoardList_Board_IdOrderByPositionAsc(Long boardId);

    @EntityGraph(attributePaths = {"boardList", "boardList.board", "boardList.board.workspace", "assignees", "createdBy", "labels"})
    Optional<Card> findWithContextById(Long id);

    @EntityGraph(attributePaths = {"boardList", "boardList.board", "boardList.board.workspace", "assignees", "createdBy", "labels"})
    List<Card> findAllWithSearchDetailsByIdIn(List<Long> ids);

    @Query(value = """
        SELECT DISTINCT c.id
        FROM cards c
        JOIN board_lists bl ON bl.id = c.list_id
        JOIN boards b ON b.id = bl.board_id
        WHERE (CAST(:boardId AS BIGINT) IS NULL OR b.id = CAST(:boardId AS BIGINT))
          AND (CAST(:workspaceId AS BIGINT) IS NULL OR b.workspace_id = CAST(:workspaceId AS BIGINT))
          AND (CAST(:query AS TEXT) IS NULL OR c.title ILIKE CONCAT('%', CAST(:query AS TEXT), '%')
               OR COALESCE(c.description, '') ILIKE CONCAT('%', CAST(:query AS TEXT), '%'))
          AND (CAST(:assigneeId AS BIGINT) IS NULL OR EXISTS (
               SELECT 1 FROM card_assignees ca WHERE ca.card_id = c.id AND ca.user_id = CAST(:assigneeId AS BIGINT)))
          AND (CAST(:label AS TEXT) IS NULL OR EXISTS (
               SELECT 1 FROM card_labels cl JOIN labels lb ON lb.id = cl.label_id
               WHERE cl.card_id = c.id AND lower(lb.name) = lower(CAST(:label AS TEXT))))
          AND (CAST(:dueFrom AS TIMESTAMPTZ) IS NULL OR c.due_date >= CAST(:dueFrom AS TIMESTAMPTZ))
          AND (CAST(:dueTo AS TIMESTAMPTZ) IS NULL OR c.due_date < CAST(:dueTo AS TIMESTAMPTZ))
          AND (CAST(:priority AS TEXT) IS NULL OR c.priority = CAST(:priority AS TEXT))
        ORDER BY c.id DESC
        """,
        countQuery = """
        SELECT COUNT(DISTINCT c.id)
        FROM cards c
        JOIN board_lists bl ON bl.id = c.list_id
        JOIN boards b ON b.id = bl.board_id
        WHERE (CAST(:boardId AS BIGINT) IS NULL OR b.id = CAST(:boardId AS BIGINT))
          AND (CAST(:workspaceId AS BIGINT) IS NULL OR b.workspace_id = CAST(:workspaceId AS BIGINT))
          AND (CAST(:query AS TEXT) IS NULL OR c.title ILIKE CONCAT('%', CAST(:query AS TEXT), '%')
               OR COALESCE(c.description, '') ILIKE CONCAT('%', CAST(:query AS TEXT), '%'))
          AND (CAST(:assigneeId AS BIGINT) IS NULL OR EXISTS (
               SELECT 1 FROM card_assignees ca WHERE ca.card_id = c.id AND ca.user_id = CAST(:assigneeId AS BIGINT)))
          AND (CAST(:label AS TEXT) IS NULL OR EXISTS (
               SELECT 1 FROM card_labels cl JOIN labels lb ON lb.id = cl.label_id
               WHERE cl.card_id = c.id AND lower(lb.name) = lower(CAST(:label AS TEXT))))
          AND (CAST(:dueFrom AS TIMESTAMPTZ) IS NULL OR c.due_date >= CAST(:dueFrom AS TIMESTAMPTZ))
          AND (CAST(:dueTo AS TIMESTAMPTZ) IS NULL OR c.due_date < CAST(:dueTo AS TIMESTAMPTZ))
          AND (CAST(:priority AS TEXT) IS NULL OR c.priority = CAST(:priority AS TEXT))
        """, nativeQuery = true)
    org.springframework.data.domain.Page<Long> searchCardIds(
        @org.springframework.data.repository.query.Param("boardId") Long boardId,
        @org.springframework.data.repository.query.Param("workspaceId") Long workspaceId,
        @org.springframework.data.repository.query.Param("query") String query,
        @org.springframework.data.repository.query.Param("assigneeId") Long assigneeId,
        @org.springframework.data.repository.query.Param("label") String label,
        @org.springframework.data.repository.query.Param("dueFrom") java.time.Instant dueFrom,
        @org.springframework.data.repository.query.Param("dueTo") java.time.Instant dueTo,
        @org.springframework.data.repository.query.Param("priority") String priority,
        org.springframework.data.domain.Pageable pageable);

    List<Card> findAllByBoardList_IdOrderByPositionAsc(Long listId);

    @Query("select coalesce(max(card.position), 0) from Card card where card.boardList.id = :listId")
    Double findMaxPosition(@Param("listId") Long listId);

    boolean existsByBoardList_Id(Long listId);
}
