package com.syncflow.backend.repository;

import com.syncflow.backend.model.Workspace;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface WorkspaceRepository extends JpaRepository<Workspace, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select workspace from Workspace workspace where workspace.id = :id")
    Optional<Workspace> findLockedById(@Param("id") Long id);
}
