package com.syncflow.backend.service;

import com.syncflow.backend.dto.*;
import com.syncflow.backend.event.ActivityEvent;
import com.syncflow.backend.event.NotificationTarget;
import com.syncflow.backend.model.*;
import com.syncflow.backend.repository.CardRepository;
import com.syncflow.backend.repository.CommentRepository;
import com.syncflow.backend.repository.WorkspaceMemberRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class CommentService {
    private static final Pattern EMAIL_MENTION = Pattern.compile("(?i)(?<![A-Za-z0-9._%+-])@([A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,})");

    private final CommentRepository commentRepository;
    private final CardRepository cardRepository;
    private final WorkspaceMemberRepository memberRepository;
    private final WorkspacePermissionService permissionService;
    private final ApplicationEventPublisher events;

    public CommentService(CommentRepository commentRepository, CardRepository cardRepository,
                          WorkspaceMemberRepository memberRepository,
                          WorkspacePermissionService permissionService, ApplicationEventPublisher events) {
        this.commentRepository = commentRepository;
        this.cardRepository = cardRepository;
        this.memberRepository = memberRepository;
        this.permissionService = permissionService;
        this.events = events;
    }

    @Transactional(readOnly = true)
    public PageResponse<CommentResponse> list(String email, Long cardId, int page, int size) {
        Card card = findCard(cardId);
        permissionService.requireMember(email, workspaceId(card));
        var result = commentRepository.findAllByCard_IdOrderByCreatedAtDescIdDesc(cardId, pageRequest(page, size));
        return new PageResponse<>(result.getContent().stream().map(this::response).toList(), page, result.getSize(),
            result.getTotalElements(), result.hasNext());
    }

    @Transactional
    public CommentResponse create(String email, Long cardId, CreateCommentRequest request) {
        Card card = findCard(cardId);
        WorkspaceMember actorMembership = permissionService.requireEditor(email, workspaceId(card));
        if (card.getBoardList().getBoard().isArchived()) throw new CardService.CardConflictException("Board is archived");
        User author = actorMembership.getUser();
        Comment comment = commentRepository.saveAndFlush(new Comment(card, author, request.getContent().trim()));

        List<NotificationTarget> mentions = resolveMentions(comment.getContent(), card, author);
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("commentId", comment.getId());
        metadata.put("cardTitle", card.getTitle());
        metadata.put("excerpt", excerpt(comment.getContent()));
        events.publishEvent(new ActivityEvent(author.getId(), workspaceId(card), boardId(card), card.getId(),
            "COMMENT_CREATED", metadata, mentions));
        return response(comment);
    }

    @Transactional
    public CommentResponse update(String email, Long commentId, UpdateCommentRequest request) {
        Comment comment = findComment(commentId);
        WorkspaceMember actor = permissionService.requireEditor(email, workspaceId(comment.getCard()));
        if (!comment.getAuthor().getId().equals(actor.getUser().getId())) {
            throw new AccessDeniedException("Only the comment author can edit it");
        }
        comment.edit(request.getContent().trim());
        commentRepository.flush();
        events.publishEvent(new ActivityEvent(actor.getUser().getId(), workspaceId(comment.getCard()),
            boardId(comment.getCard()), comment.getCard().getId(), "COMMENT_EDITED",
            Map.of("commentId", comment.getId(), "cardTitle", comment.getCard().getTitle()), List.of()));
        return response(comment);
    }

    @Transactional
    public void delete(String email, Long commentId) {
        Comment comment = findComment(commentId);
        Card card = comment.getCard();
        WorkspaceMember actor = permissionService.requireEditor(email, workspaceId(card));
        if (!comment.getAuthor().getId().equals(actor.getUser().getId()) && actor.getRole() != WorkspaceRole.OWNER) {
            throw new AccessDeniedException("Only the comment author or workspace Owner can delete it");
        }
        Long commentIdValue = comment.getId();
        String cardTitle = card.getTitle();
        Long cardId = card.getId();
        Long workspaceId = workspaceId(card);
        Long boardId = boardId(card);
        commentRepository.delete(comment);
        events.publishEvent(new ActivityEvent(actor.getUser().getId(), workspaceId, boardId, cardId,
            "COMMENT_DELETED", Map.of("commentId", commentIdValue, "cardTitle", cardTitle), List.of()));
    }

    private List<NotificationTarget> resolveMentions(String content, Card card, User author) {
        Set<String> mentionedEmails = new HashSet<>();
        Matcher matcher = EMAIL_MENTION.matcher(content);
        while (matcher.find()) mentionedEmails.add(matcher.group(1).toLowerCase(Locale.ROOT));
        if (mentionedEmails.isEmpty()) return List.of();

        return memberRepository.findAllByWorkspace_IdOrderByJoinedAtAsc(workspaceId(card)).stream()
            .map(WorkspaceMember::getUser)
            .filter(user -> !user.getId().equals(author.getId()) && mentionedEmails.contains(user.getEmail().toLowerCase(Locale.ROOT)))
            .map(user -> new NotificationTarget(user.getId(), "MENTION",
                author.getFullName() + " mentioned you in a comment on “" + card.getTitle() + "”.",
                Map.of("cardId", card.getId(), "cardTitle", card.getTitle())))
            .toList();
    }

    private PageRequest pageRequest(int page, int size) {
        if (page < 0) throw new IllegalArgumentException("page must be zero or greater");
        if (size < 1 || size > 100) throw new IllegalArgumentException("size must be between 1 and 100");
        return PageRequest.of(page, size, Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id")));
    }

    private Card findCard(Long id) { return cardRepository.findWithContextById(id).orElseThrow(CardService.CardNotFoundException::new); }
    private Comment findComment(Long id) { return commentRepository.findById(id).orElseThrow(CommentNotFoundException::new); }
    private Long workspaceId(Card card) { return card.getBoardList().getBoard().getWorkspace().getId(); }
    private Long boardId(Card card) { return card.getBoardList().getBoard().getId(); }
    private String excerpt(String text) { return text.length() <= 160 ? text : text.substring(0, 157) + "..."; }

    private CommentResponse response(Comment comment) {
        User author = comment.getAuthor();
        return new CommentResponse(comment.getId(), comment.getCard().getId(), author.getId(), author.getFullName(),
            author.getEmail(), comment.getContent(), comment.getCreatedAt(), comment.getEditedAt());
    }

    public static class CommentNotFoundException extends RuntimeException {
        public CommentNotFoundException() { super("Comment not found"); }
    }
}
