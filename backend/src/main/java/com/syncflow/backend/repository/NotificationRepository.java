package com.syncflow.backend.repository;

import com.syncflow.backend.model.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    Page<Notification> findAllByUser_IdOrderByCreatedAtDescIdDesc(Long userId, Pageable pageable);
    long countByUser_IdAndReadFalse(Long userId);
    @Modifying
    @Query(value = "UPDATE notifications SET is_read = TRUE WHERE user_id = :userId AND is_read = FALSE", nativeQuery = true)
    int markAllRead(@Param("userId") Long userId);
}
