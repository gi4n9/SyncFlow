package com.syncflow.backend.service;

import com.syncflow.backend.dto.*;
import com.syncflow.backend.model.*;
import com.syncflow.backend.repository.BoardListRepository;
import com.syncflow.backend.repository.CardRepository;
import com.syncflow.backend.repository.UserRepository;
import com.syncflow.backend.repository.WorkspaceMemberRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class CardService {
    private final CardRepository cardRepository;
    private final BoardListRepository listRepository;
    private final UserRepository userRepository;
    private final WorkspaceMemberRepository memberRepository;
    private final WorkspacePermissionService permissionService;

    public CardService(CardRepository cardRepository, BoardListRepository listRepository,
                       UserRepository userRepository, WorkspaceMemberRepository memberRepository,
                       WorkspacePermissionService permissionService) {
        this.cardRepository = cardRepository;
        this.listRepository = listRepository;
        this.userRepository = userRepository;
        this.memberRepository = memberRepository;
        this.permissionService = permissionService;
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
        return cardResponse(cardRepository.saveAndFlush(card));
    }

    @Transactional
    public CardSummaryResponse update(String email, Long cardId, UpdateCardRequest request) {
        Card card = findCard(cardId);
        permissionService.requireEditor(email, workspaceId(card));
        requireVersion(card, request.getVersion());
        if (card.getBoardList().getBoard().isArchived()) throw new CardConflictException("Board is archived");
        card.update(request.getTitle().trim(), clean(request.getDescription()), request.getDueDate(), request.getPriority());
        return cardResponse(cardRepository.saveAndFlush(card));
    }

    @Transactional
    public CardSummaryResponse move(String email, Long cardId, MoveCardRequest request) {
        Card card = findCard(cardId);
        BoardList destination = findList(request.getListId());
        permissionService.requireEditor(email, workspaceId(card));
        requireVersion(card, request.getVersion());
        if (!card.getBoardList().getBoard().getId().equals(destination.getBoard().getId())) {
            throw new InvalidCardRelationException("Destination list must belong to the same board");
        }
        if (destination.getBoard().isArchived()) throw new CardConflictException("Board is archived");

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
        cardRepository.delete(card);
    }

    @Transactional
    public CardSummaryResponse setAssignees(String email, Long cardId, SetCardAssigneesRequest request) {
        Card card = findCard(cardId);
        permissionService.requireEditor(email, workspaceId(card));
        requireVersion(card, request.getVersion());
        Set<Long> userIds = request.getUserIds();
        List<WorkspaceMember> memberships = userIds.isEmpty() ? List.of()
            : memberRepository.findAllByWorkspace_IdAndUser_IdIn(workspaceId(card), userIds);
        if (memberships.size() != userIds.size()) {
            throw new InvalidCardRelationException("Every assignee must belong to the workspace");
        }
        card.setAssignees(memberships.stream().map(WorkspaceMember::getUser).collect(Collectors.toSet()));
        return cardResponse(cardRepository.saveAndFlush(card));
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
        Long creatorId = card.getCreatedBy() == null ? null : card.getCreatedBy().getId();
        return new CardSummaryResponse(card.getId(), card.getTitle(), card.getDescription(), card.getPosition(),
            card.getDueDate(), card.getPriority(), card.getVersion(), creatorId, assignees);
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
