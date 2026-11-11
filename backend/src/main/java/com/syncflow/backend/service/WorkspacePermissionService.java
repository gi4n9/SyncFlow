package com.syncflow.backend.service;

import com.syncflow.backend.model.Workspace;
import com.syncflow.backend.model.WorkspaceMember;
import com.syncflow.backend.model.WorkspaceRole;
import com.syncflow.backend.repository.WorkspaceMemberRepository;
import com.syncflow.backend.repository.WorkspaceRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.util.Locale;

@Service
public class WorkspacePermissionService {
    private final WorkspaceRepository workspaceRepository;
    private final WorkspaceMemberRepository memberRepository;

    public WorkspacePermissionService(WorkspaceRepository workspaceRepository,
                                      WorkspaceMemberRepository memberRepository) {
        this.workspaceRepository = workspaceRepository;
        this.memberRepository = memberRepository;
    }

    public WorkspaceMember requireMember(String email, Long workspaceId) {
        return memberRepository.findByWorkspace_IdAndUser_Email(workspaceId, normalizeEmail(email))
            .orElseThrow(WorkspaceService.WorkspaceNotFoundException::new);
    }

    public WorkspaceMember requireEditor(String email, Long workspaceId) {
        WorkspaceMember member = requireMember(email, workspaceId);
        if (member.getRole() == WorkspaceRole.VIEWER) {
            throw new AccessDeniedException("Workspace edit permission is required");
        }
        return member;
    }

    public Workspace requireOwner(String email, Long workspaceId) {
        Workspace workspace = workspaceRepository.findLockedById(workspaceId)
            .orElseThrow(WorkspaceService.WorkspaceNotFoundException::new);
        WorkspaceMember member = requireMember(email, workspaceId);
        if (member.getRole() != WorkspaceRole.OWNER || !member.getUser().getId().equals(workspace.getOwner().getId())) {
            throw new AccessDeniedException("Workspace owner permission is required");
        }
        return workspace;
    }

    private String normalizeEmail(String email) {
        if (email == null) throw new WorkspaceService.WorkspaceNotFoundException();
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
