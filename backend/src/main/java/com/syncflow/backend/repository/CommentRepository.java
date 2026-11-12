package com.syncflow.backend.repository;

import com.syncflow.backend.model.Comment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommentRepository extends JpaRepository<Comment, Long> {
    @EntityGraph(attributePaths = {"author", "card"})
    Page<Comment> findAllByCard_IdOrderByCreatedAtDescIdDesc(Long cardId, Pageable pageable);
}
