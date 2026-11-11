package com.syncflow.backend.repository;

import com.syncflow.backend.model.BoardList;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface BoardListRepository extends JpaRepository<BoardList, Long> {
    @EntityGraph(attributePaths = "board")
    List<BoardList> findAllByBoard_IdOrderByPositionAsc(Long boardId);

    @EntityGraph(attributePaths = "board")
    Optional<BoardList> findWithBoardById(Long id);

    @Query("select coalesce(max(boardList.position), 0) from BoardList boardList where boardList.board.id = :boardId")
    Double findMaxPosition(@Param("boardId") Long boardId);
}
