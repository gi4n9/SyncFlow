package com.syncflow.backend.service;

import com.syncflow.backend.dto.*;
import com.syncflow.backend.event.ActivityEvent;
import com.syncflow.backend.event.NotificationTarget;
import com.syncflow.backend.model.*;
import com.syncflow.backend.repository.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.List;
import java.util.Locale;

@Service
public class WorkspaceService {
    private final WorkspaceRepository workspaceRepository;
    private final WorkspaceMemberRepository memberRepository;
    private final WorkspaceInvitationRepository invitationRepository;
    private final UserRepository userRepository;
    private final WorkspacePermissionService permissionService;
    private final ApplicationEventPublisher events;
    private final SecureRandom secureRandom = new SecureRandom();

    public WorkspaceService(WorkspaceRepository workspaceRepository,
                            WorkspaceMemberRepository memberRepository,
                            WorkspaceInvitationRepository invitationRepository,
                            UserRepository userRepository,
                            WorkspacePermissionService permissionService,
                            ApplicationEventPublisher events) {
        this.workspaceRepository = workspaceRepository;
        this.memberRepository = memberRepository;
        this.invitationRepository = invitationRepository;
        this.userRepository = userRepository;
        this.permissionService = permissionService;
        this.events = events;
    }

    @Transactional
    public WorkspaceResponse create(String email, CreateWorkspaceRequest request) {
        User user = findUser(email);
        Workspace workspace = workspaceRepository.saveAndFlush(
            new Workspace(request.getName().trim(), cleanDescription(request.getDescription()), user));
        memberRepository.save(new WorkspaceMember(workspace, user, WorkspaceRole.OWNER));
        return response(workspace, WorkspaceRole.OWNER);
    }

    @Transactional(readOnly = true)
    public List<WorkspaceResponse> listMine(String email) {
        return memberRepository.findAllForUser(normalizeEmail(email)).stream()
            .map(member -> response(member.getWorkspace(), member.getRole())).toList();
    }

    @Transactional(readOnly = true)
    public WorkspaceResponse get(String email, Long workspaceId) {
        WorkspaceMember member = requireMember(email, workspaceId);
        return response(member.getWorkspace(), member.getRole());
    }

    @Transactional
    public WorkspaceResponse update(String email, Long workspaceId, UpdateWorkspaceRequest request) {
        Workspace workspace = requireOwner(email, workspaceId);
        workspace.setName(request.getName().trim());
        workspace.setDescription(cleanDescription(request.getDescription()));
        workspace.touch();
        return response(workspaceRepository.save(workspace), WorkspaceRole.OWNER);
    }

    @Transactional
    public void delete(String email, Long workspaceId) {
        Workspace workspace = requireOwner(email, workspaceId);
        workspaceRepository.delete(workspace); // Hard delete; child rows use ON DELETE CASCADE.
    }

    @Transactional(readOnly = true)
    public List<WorkspaceMemberResponse> listMembers(String email, Long workspaceId) {
        requireMember(email, workspaceId);
        return memberRepository.findAllByWorkspace_IdOrderByJoinedAtAsc(workspaceId).stream()
            .map(this::memberResponse).toList();
    }

    @Transactional
    public InvitationResponse invite(String email, Long workspaceId, InviteMemberRequest request) {
        Workspace workspace = requireOwner(email, workspaceId);
        String invitedEmail = normalizeEmail(request.getEmail());
        if (request.getRole() == WorkspaceRole.OWNER) {
            throw new IllegalArgumentException("Use role transfer to assign an owner");
        }
        if (memberRepository.findByWorkspace_IdAndUser_Email(workspaceId, invitedEmail).isPresent()) {
            throw new WorkspaceConflictException("User is already a workspace member");
        }
        var pending = invitationRepository.findByWorkspace_IdAndEmailAndStatus(
            workspaceId, invitedEmail, InvitationStatus.PENDING);
        if (pending.isPresent()) {
            if (pending.get().getExpiresAt().isAfter(Instant.now())) {
                throw new WorkspaceConflictException("A pending invitation already exists for this email");
            }
            pending.get().setStatus(InvitationStatus.EXPIRED);
            invitationRepository.flush();
        }

        User owner = findUser(email);
        String token = createToken();
        Instant expiresAt = Instant.now().plus(7, ChronoUnit.DAYS);
        WorkspaceInvitation invitation = invitationRepository.saveAndFlush(new WorkspaceInvitation(workspace, owner, invitedEmail,
            request.getRole(), hash(token), expiresAt));
        List<NotificationTarget> notifications = userRepository.findByEmail(invitedEmail)
            .filter(invitedUser -> !invitedUser.getId().equals(owner.getId()))
            .map(invitedUser -> List.of(new NotificationTarget(invitedUser.getId(), "WORKSPACE_INVITATION",
                owner.getFullName() + " invited you to “" + workspace.getName() + "”.",
                java.util.Map.of("workspaceId", workspaceId, "workspaceName", workspace.getName(),
                    "invitationId", invitation.getId()))))
            .orElse(List.of());
        events.publishEvent(new ActivityEvent(owner.getId(), workspaceId, null, null, "MEMBER_INVITED",
            java.util.Map.of("email", invitedEmail, "role", request.getRole().name()), notifications));
        return new InvitationResponse(workspaceId, invitedEmail, request.getRole(), token, expiresAt);
    }

    @Transactional(noRollbackFor = InvalidInvitationException.class)
    public InvitationAcceptedResponse acceptInvitation(String email, String rawToken) {
        User user = findUser(email);
        WorkspaceInvitation invitation = invitationRepository.findByTokenHash(hash(rawToken))
            .orElseThrow(InvalidInvitationException::new);
        return acceptInvitation(email, user, invitation);
    }

    @Transactional(noRollbackFor = InvalidInvitationException.class)
    public InvitationAcceptedResponse acceptInvitationById(String email, Long invitationId) {
        User user = findUser(email);
        WorkspaceInvitation invitation = invitationRepository.findLockedById(invitationId)
            .orElseThrow(InvalidInvitationException::new);
        return acceptInvitation(email, user, invitation);
    }

    @Transactional(noRollbackFor = InvalidInvitationException.class)
    public void declineInvitation(String email, Long invitationId) {
        User user = findUser(email);
        WorkspaceInvitation invitation = invitationRepository.findLockedById(invitationId)
            .orElseThrow(InvalidInvitationException::new);
        verifyPendingInvitation(email, user, invitation);
        invitation.setStatus(InvitationStatus.DECLINED);
        events.publishEvent(new ActivityEvent(user.getId(), invitation.getWorkspace().getId(), null, null,
            "INVITATION_DECLINED", java.util.Map.of("memberId", user.getId(), "memberName", user.getFullName()), List.of()));
    }

    private InvitationAcceptedResponse acceptInvitation(String email, User user, WorkspaceInvitation invitation) {
        verifyPendingInvitation(email, user, invitation);
        if (memberRepository.existsByWorkspace_IdAndUser_Id(invitation.getWorkspace().getId(), user.getId())) {
            throw new WorkspaceConflictException("User is already a workspace member");
        }

        memberRepository.save(new WorkspaceMember(invitation.getWorkspace(), user, invitation.getRole()));
        invitation.setStatus(InvitationStatus.ACCEPTED);
        User owner = invitation.getWorkspace().getOwner();
        List<NotificationTarget> notifications = owner.getId().equals(user.getId()) ? List.of()
            : List.of(new NotificationTarget(owner.getId(), "WORKSPACE_MEMBER_JOINED",
                user.getFullName() + " joined “" + invitation.getWorkspace().getName() + "”.",
                java.util.Map.of("workspaceId", invitation.getWorkspace().getId(), "memberId", user.getId())));
        events.publishEvent(new ActivityEvent(user.getId(), invitation.getWorkspace().getId(), null, null,
            "MEMBER_JOINED", java.util.Map.of("memberId", user.getId(), "memberName", user.getFullName(),
                "role", invitation.getRole().name()), notifications));
        return new InvitationAcceptedResponse(invitation.getWorkspace().getId(),
            invitation.getWorkspace().getName(), invitation.getRole().name());
    }

    private void verifyPendingInvitation(String email, User user, WorkspaceInvitation invitation) {
        if (invitation.getStatus() != InvitationStatus.PENDING) {
            throw new InvalidInvitationException();
        }
        if (!invitation.getExpiresAt().isAfter(Instant.now())) {
            invitation.setStatus(InvitationStatus.EXPIRED);
            throw new InvalidInvitationException();
        }
        if (!invitation.getEmail().equals(normalizeEmail(user.getEmail()))) {
            throw new InvitationEmailMismatchException();
        }
    }

    @Transactional
    public WorkspaceMemberResponse updateMemberRole(String email, Long workspaceId, Long userId, UpdateMemberRoleRequest request) {
        Workspace workspace = requireOwner(email, workspaceId);
        WorkspaceMember target = memberRepository.findByWorkspace_IdAndUser_Id(workspaceId, userId)
            .orElseThrow(WorkspaceNotFoundException::new);
        WorkspaceRole requestedRole = request.getRole();

        if (target.getUser().getId().equals(workspace.getOwner().getId()) && requestedRole != WorkspaceRole.OWNER) {
            throw new WorkspaceConflictException("Transfer ownership before changing the current owner's role");
        }
        if (requestedRole == WorkspaceRole.OWNER && !target.getUser().getId().equals(workspace.getOwner().getId())) {
            WorkspaceMember currentOwner = memberRepository.findByWorkspace_IdAndUser_Id(
                workspaceId, workspace.getOwner().getId()).orElseThrow(WorkspaceNotFoundException::new);
            currentOwner.setRole(WorkspaceRole.MEMBER);
            target.setRole(WorkspaceRole.OWNER);
            workspace.setOwner(target.getUser());
            workspace.touch();
        } else {
            target.setRole(requestedRole);
        }
        memberRepository.save(target);
        events.publishEvent(new ActivityEvent(findUser(email).getId(), workspaceId, null, null,
            "MEMBER_ROLE_CHANGED", java.util.Map.of("memberId", target.getUser().getId(),
                "memberName", target.getUser().getFullName(), "role", requestedRole.name()), List.of()));
        return memberResponse(target);
    }

    @Transactional
    public void removeMember(String email, Long workspaceId, Long userId) {
        Workspace workspace = requireOwner(email, workspaceId);
        if (workspace.getOwner().getId().equals(userId)) {
            throw new WorkspaceConflictException("Transfer ownership before removing the owner");
        }
        WorkspaceMember target = memberRepository.findByWorkspace_IdAndUser_Id(workspaceId, userId)
            .orElseThrow(WorkspaceNotFoundException::new);
        User actor = findUser(email);
        events.publishEvent(new ActivityEvent(actor.getId(), workspaceId, null, null, "MEMBER_REMOVED",
            java.util.Map.of("memberId", target.getUser().getId(), "memberName", target.getUser().getFullName()), List.of()));
        memberRepository.delete(target);
    }

    @Transactional
    public void leave(String email, Long workspaceId) {
        Workspace workspace = lockWorkspace(workspaceId);
        WorkspaceMember current = memberRepository.findByWorkspace_IdAndUser_Email(workspaceId, normalizeEmail(email))
            .orElseThrow(WorkspaceNotFoundException::new);
        if (current.getUser().getId().equals(workspace.getOwner().getId())) {
            throw new WorkspaceConflictException("Transfer ownership before leaving the workspace");
        }
        events.publishEvent(new ActivityEvent(current.getUser().getId(), workspaceId, null, null, "MEMBER_LEFT",
            java.util.Map.of("memberId", current.getUser().getId(), "memberName", current.getUser().getFullName()), List.of()));
        memberRepository.delete(current);
    }

    private Workspace requireOwner(String email, Long workspaceId) {
        return permissionService.requireOwner(email, workspaceId);
    }

    private WorkspaceMember requireMember(String email, Long workspaceId) {
        return permissionService.requireMember(email, workspaceId);
    }

    private Workspace lockWorkspace(Long workspaceId) {
        return workspaceRepository.findLockedById(workspaceId).orElseThrow(WorkspaceNotFoundException::new);
    }

    private User findUser(String email) {
        return userRepository.findByEmail(normalizeEmail(email)).orElseThrow(AuthService.UserNotFoundException::new);
    }

    private WorkspaceResponse response(Workspace workspace, WorkspaceRole role) {
        return new WorkspaceResponse(workspace.getId(), workspace.getName(), workspace.getDescription(),
            workspace.getOwner().getId(), role.name());
    }

    private WorkspaceMemberResponse memberResponse(WorkspaceMember member) {
        User user = member.getUser();
        return new WorkspaceMemberResponse(user.getId(), user.getEmail(), user.getFullName(),
            member.getRole().name(), member.getJoinedAt());
    }

    private String normalizeEmail(String email) {
        if (email == null || email.isBlank()) throw new IllegalArgumentException("Email is required");
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private String cleanDescription(String description) {
        return description == null || description.isBlank() ? null : description.trim();
    }

    private String createToken() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hash(String token) {
        try {
            return java.util.HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException("Could not hash invitation token", exception);
        }
    }

    public static class WorkspaceNotFoundException extends RuntimeException {
        public WorkspaceNotFoundException() { super("Workspace or membership not found"); }
    }

    public static class WorkspaceConflictException extends RuntimeException {
        public WorkspaceConflictException(String message) { super(message); }
    }

    public static class InvalidInvitationException extends RuntimeException {
        public InvalidInvitationException() { super("Invitation is invalid, expired, or already used"); }
    }

    public static class InvitationEmailMismatchException extends AccessDeniedException {
        public InvitationEmailMismatchException() {
            super("Lời mời được gửi tới email khác. Hãy đăng nhập bằng email nhận lời mời.");
        }
    }
}
