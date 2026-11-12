package com.syncflow.backend.repository;

import com.syncflow.backend.model.Label;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface LabelRepository extends JpaRepository<Label, Long> {
    Optional<Label> findByBoard_IdAndNameIgnoreCase(Long boardId, String name);
    List<Label> findAllByBoard_IdOrderByNameAsc(Long boardId);
    List<Label> findAllByBoard_Workspace_IdOrderByNameAsc(Long workspaceId);
}
