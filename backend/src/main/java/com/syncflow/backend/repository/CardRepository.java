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
        "left join fetch card.assignees left join fetch card.createdBy " +
        "where boardList.board.id = :boardId order by card.position asc")
    List<Card> findAllByBoardList_Board_IdOrderByPositionAsc(Long boardId);

    @EntityGraph(attributePaths = {"boardList", "boardList.board", "boardList.board.workspace", "assignees", "createdBy"})
    Optional<Card> findWithContextById(Long id);

    List<Card> findAllByBoardList_IdOrderByPositionAsc(Long listId);

    @Query("select coalesce(max(card.position), 0) from Card card where card.boardList.id = :listId")
    Double findMaxPosition(@Param("listId") Long listId);

    boolean existsByBoardList_Id(Long listId);
}
