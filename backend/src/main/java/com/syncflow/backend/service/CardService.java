package com.syncflow.backend.service;

import com.syncflow.backend.dto.*;
import com.syncflow.backend.event.ActivityEvent;
import com.syncflow.backend.event.NotificationTarget;
import com.syncflow.backend.model.*;
import com.syncflow.backend.repository.BoardListRepository;
import com.syncflow.backend.repository.CardRepository;
import com.syncflow.backend.repository.LabelRepository;
import com.syncflow.backend.repository.UserRepository;
import com.syncflow.backend.repository.WorkspaceMemberRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class CardService {
    private final CardRepository cardRepository;
    private final BoardListRepository listRepository;
    private final UserRepository userRepository;
    private final WorkspaceMemberRepository memberRepository;
    private final WorkspacePermissionService permissionService;
    private final ApplicationEventPublisher events;
    private final LabelRepository labelRepository;

    public CardService(CardRepository cardRepository, BoardListRepository listRepository,
                       UserRepository userRepository, WorkspaceMemberRepository memberRepository,
                       WorkspacePermissionService permissionService, ApplicationEventPublisher events,
                       LabelRepository labelRepository) {
        this.cardRepository = cardRepository;
        this.listRepository = listRepository;
        this.userRepository = userRepository;
        this.memberRepository = memberRepository;
        this.permissionService = permissionService;
        this.events = events;
        this.labelRepository = labelRepository;
    }

    @Transactional
    public CardSummaryResponse create(String email, Long listId, CreateCardRequest request) {
        BoardList list = findList(listId);
        permissionService.requireEditor(email, list.getBoard().getWorkspace().getId());
        if (list.getBoard().isArchived()) throw new CardConflictException("Cannot add a card to an archived board");
        User creator = userRepository.findByEmail(email).orElseThrow(AuthService.UserNotFoundException::new);
        double position = cardRepository.findMaxPosition(listId) + 1000.0;
        Card card = new Card(list, request.getTitle().trim(), clean(request.getDescription()), position,
            request.getDueDate(), request.getPriority() == null ? CardPriority.MEDIUM : request.getPriority(), creator);
        Card created = cardRepository.saveAndFlush(card);
        events.publishEvent(new ActivityEvent(creator.getId(), list.getBoard().getWorkspace().getId(),
            list.getBoard().getId(), created.getId(), "CARD_CREATED",
            Map.of("title", created.getTitle(), "listId", list.getId(), "listName", list.getName()), List.of()));
        return cardResponse(created);
    }

    @Transactional
    public CardSummaryResponse update(String email, Long cardId, UpdateCardRequest request) {
        Card card = findCard(cardId);
        permissionService.requireEditor(email, workspaceId(card));
        requireVersion(card, request.getVersion());
        if (card.getBoardList().getBoard().isArchived()) throw new CardConflictException("Board is archived");
        card.update(request.getTitle().trim(), clean(request.getDescription()), request.getDueDate(), request.getPriority());
        Card updated = cardRepository.saveAndFlush(card);
        User actor = userRepository.findByEmail(email).orElseThrow(AuthService.UserNotFoundException::new);
        events.publishEvent(new ActivityEvent(actor.getId(), workspaceId(updated),
            updated.getBoardList().getBoard().getId(), updated.getId(), "CARD_UPDATED",
            Map.of("title", updated.getTitle()), List.of()));
        return cardResponse(updated);
    }

    @Transactional
    public CardSummaryResponse move(String email, Long cardId, MoveCardRequest request) {
        Card card = findCard(cardId);
        BoardList destination = findList(request.getListId());
        WorkspaceMember actor = permissionService.requireEditor(email, workspaceId(card));
        requireVersion(card, request.getVersion());
        if (!card.getBoardList().getBoard().getId().equals(destination.getBoard().getId())) {
            throw new InvalidCardRelationException("Destination list must belong to the same board");
        }
        if (destination.getBoard().isArchived()) throw new CardConflictException("Board is archived");

        BoardList source = card.getBoardList();
        String sourceListName = source.getName();
        card.moveTo(destination, request.getPosition());
        List<Card> destinationCards = cardRepository.findAllByBoardList_IdOrderByPositionAsc(destination.getId());
        destinationCards.sort(Comparator.comparing(Card::getPosition).thenComparing(Card::getId));
        boolean rebalance = false;
        for (int index = 1; index < destinationCards.size(); index++) {
            if (destinationCards.get(index).getPosition() - destinationCards.get(index - 1).getPosition() < 0.001) {
                rebalance = true;
                break;
            }
        }
        if (rebalance) {
            for (int index = 0; index < destinationCards.size(); index++) {
                destinationCards.get(index).setPosition((index + 1) * 1000.0);
            }
            cardRepository.saveAllAndFlush(destinationCards);
        } else {
            cardRepository.saveAndFlush(card);
        }
        events.publishEvent(new ActivityEvent(actor.getUser().getId(), workspaceId(card),
            destination.getBoard().getId(), card.getId(), "CARD_MOVED",
            Map.of("title", card.getTitle(), "fromList", sourceListName, "toList", destination.getName()), List.of()));
        return cardResponse(card);
    }

    @Transactional
    public void delete(String email, Long cardId) {
        Card card = findCard(cardId);
        WorkspaceMember member = permissionService.requireEditor(email, workspaceId(card));
        if (member.getRole() != WorkspaceRole.OWNER &&
            (card.getCreatedBy() == null || !card.getCreatedBy().getId().equals(member.getUser().getId()))) {
            throw new AccessDeniedException("Only the card creator or a workspace Owner can delete this card");
        }
        events.publishEvent(new ActivityEvent(member.getUser().getId(), workspaceId(card),
            card.getBoardList().getBoard().getId(), card.getId(), "CARD_DELETED",
            Map.of("title", card.getTitle(), "listName", card.getBoardList().getName()), List.of()));
        cardRepository.delete(card);
    }

    @Transactional
    public CardSummaryResponse setAssignees(String email, Long cardId, SetCardAssigneesRequest request) {
        Card card = findCard(cardId);
        WorkspaceMember actor = permissionService.requireEditor(email, workspaceId(card));
        requireVersion(card, request.getVersion());
        Set<Long> previousIds = card.getAssignees().stream().map(User::getId).collect(Collectors.toSet());
        Set<Long> userIds = request.getUserIds();
        List<WorkspaceMember> memberships = userIds.isEmpty() ? List.of()
            : memberRepository.findAllByWorkspace_IdAndUser_IdIn(workspaceId(card), userIds);
        if (memberships.size() != userIds.size()) {
            throw new InvalidCardRelationException("Every assignee must belong to the workspace");
        }
        Set<User> assignees = memberships.stream().map(WorkspaceMember::getUser).collect(Collectors.toSet());
        Set<Long> nextIds = assignees.stream().map(User::getId).collect(Collectors.toSet());
        if (!previousIds.equals(nextIds)) {
            card.setAssignees(assignees);
            Card saved = cardRepository.saveAndFlush(card);
            List<NotificationTarget> notifications = memberships.stream()
                .map(WorkspaceMember::getUser)
                .filter(user -> nextIds.contains(user.getId()) && !previousIds.contains(user.getId())
                    && !user.getId().equals(actor.getUser().getId()))
                .map(user -> new NotificationTarget(user.getId(), "CARD_ASSIGNED",
                    actor.getUser().getFullName() + " assigned you to “" + saved.getTitle() + "”.",
                    Map.of("cardId", saved.getId(), "cardTitle", saved.getTitle())))
                .toList();
            events.publishEvent(new ActivityEvent(actor.getUser().getId(), workspaceId(saved),
                saved.getBoardList().getBoard().getId(), saved.getId(), "ASSIGNEES_CHANGED",
                Map.of("cardTitle", saved.getTitle(), "assigneeIds", nextIds), notifications));
            return cardResponse(saved);
        }
        return cardResponse(card);
    }

    @Transactional
    public CardSummaryResponse setLabels(String email, Long cardId, SetCardLabelsRequest request) {
        Card card = findCard(cardId);
        WorkspaceMember actor = permissionService.requireEditor(email, workspaceId(card));
        if (card.getBoardList().getBoard().isArchived()) throw new CardConflictException("Board is archived");
        Long boardId = card.getBoardList().getBoard().getId();
        Map<String, String> normalized = request.getLabels().stream().filter(name -> name != null)
            .map(String::trim).filter(name -> !name.isBlank())
            .collect(Collectors.toMap(name -> name.toLowerCase(java.util.Locale.ROOT), name -> name,
                (first, ignored) -> first, java.util.LinkedHashMap::new));
        Set<Label> labels = normalized.values().stream().map(name -> labelRepository.findByBoard_IdAndNameIgnoreCase(boardId, name)
            .orElseGet(() -> labelRepository.save(new Label(card.getBoardList().getBoard(), name))))
            .collect(Collectors.toSet());
        Set<Long> oldIds = card.getLabels().stream().map(Label::getId).collect(Collectors.toSet());
        Set<Long> newIds = labels.stream().map(Label::getId).collect(Collectors.toSet());
        if (!oldIds.equals(newIds)) {
            card.setLabels(labels);
            Card saved = cardRepository.saveAndFlush(card);
            events.publishEvent(new ActivityEvent(actor.getUser().getId(), workspaceId(saved), boardId,
                saved.getId(), "CARD_LABELS_CHANGED", Map.of("title", saved.getTitle(), "labels", normalized.values().stream().toList()), List.of()));
            return cardResponse(saved);
        }
        return cardResponse(card);
    }

    private Card findCard(Long cardId) {
        return cardRepository.findWithContextById(cardId).orElseThrow(CardNotFoundException::new);
    }

    private BoardList findList(Long listId) {
        return listRepository.findWithBoardById(listId).orElseThrow(BoardService.BoardNotFoundException::new);
    }

    private Long workspaceId(Card card) {
        return card.getBoardList().getBoard().getWorkspace().getId();
    }

    private void requireVersion(Card card, Long expectedVersion) {
        if (expectedVersion == null || card.getVersion() != expectedVersion) {
            throw new CardConflictException("This card changed since it was loaded. Reload and try again.");
        }
    }

    private String clean(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private CardSummaryResponse cardResponse(Card card) {
        List<CardAssigneeResponse> assignees = card.getAssignees().stream()
            .map(user -> new CardAssigneeResponse(user.getId(), user.getEmail(), user.getFullName())).toList();
        List<CardLabelResponse> labels = card.getLabels().stream()
            .map(label -> new CardLabelResponse(label.getId(), label.getName(), label.getColor())).toList();
        Long creatorId = card.getCreatedBy() == null ? null : card.getCreatedBy().getId();
        return new CardSummaryResponse(card.getId(), card.getTitle(), card.getDescription(), card.getPosition(),
            card.getDueDate(), card.getPriority(), card.getVersion(), creatorId, assignees, labels);
    }

    public static class CardNotFoundException extends RuntimeException {
        public CardNotFoundException() { super("Card not found"); }
    }

    public static class CardConflictException extends RuntimeException {
        public CardConflictException(String message) { super(message); }
    }

    public static class InvalidCardRelationException extends RuntimeException {
        public InvalidCardRelationException(String message) { super(message); }
    }
}
