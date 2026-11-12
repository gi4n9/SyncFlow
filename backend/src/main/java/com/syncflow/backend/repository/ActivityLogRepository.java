package com.syncflow.backend.repository;

import com.syncflow.backend.model.ActivityLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ActivityLogRepository extends JpaRepository<ActivityLog, Long> {
    Page<ActivityLog> findAllByBoard_IdOrderByCreatedAtDescIdDesc(Long boardId, Pageable pageable);
    Page<ActivityLog> findAllByCard_IdOrderByCreatedAtDescIdDesc(Long cardId, Pageable pageable);
}
