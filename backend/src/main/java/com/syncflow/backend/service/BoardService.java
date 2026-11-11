package com.syncflow.backend.service;

import com.syncflow.backend.dto.*;
import com.syncflow.backend.model.Board;
import com.syncflow.backend.model.BoardList;
import com.syncflow.backend.model.Card;
import com.syncflow.backend.repository.BoardListRepository;
import com.syncflow.backend.repository.BoardRepository;
import com.syncflow.backend.repository.CardRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class BoardService {
    private static final List<String> DEFAULT_LISTS = List.of("To do", "In progress", "Done");
    private final BoardRepository boardRepository;
    private final BoardListRepository listRepository;
    private final CardRepository cardRepository;
    private final WorkspacePermissionService permissionService;

    public BoardService(BoardRepository boardRepository, BoardListRepository listRepository,
                        CardRepository cardRepository, WorkspacePermissionService permissionService) {
        this.boardRepository = boardRepository;
        this.listRepository = listRepository;
        this.cardRepository = cardRepository;
        this.permissionService = permissionService;
    }

    @Transactional
    public BoardResponse create(String email, Long workspaceId, CreateBoardRequest request) {
        var member = permissionService.requireEditor(email, workspaceId);
        int position = boardRepository.findMaxPosition(workspaceId) + 1;
        Board board = boardRepository.saveAndFlush(
            new Board(member.getWorkspace(), request.getName().trim(), position));
        for (int index = 0; index < DEFAULT_LISTS.size(); index++) {
            listRepository.save(new BoardList(board, DEFAULT_LISTS.get(index), (index + 1) * 1000.0));
        }
        return boardDetails(email, board.getId());
    }

    @Transactional(readOnly = true)
    public List<BoardSummaryResponse> list(String email, Long workspaceId, boolean includeArchived) {
        permissionService.requireMember(email, workspaceId);
        List<Board> boards = includeArchived
            ? boardRepository.findAllByWorkspace_IdOrderByPositionAsc(workspaceId)
            : boardRepository.findAllByWorkspace_IdAndArchivedFalseOrderByPositionAsc(workspaceId);
        return boards.stream().map(this::summary).toList();
    }

    @Transactional(readOnly = true)
    public BoardResponse get(String email, Long boardId) {
        return boardDetails(email, boardId);
    }

    @Transactional
    public BoardResponse update(String email, Long boardId, UpdateBoardRequest request) {
        Board board = findBoard(boardId);
        permissionService.requireEditor(email, board.getWorkspace().getId());
        board.setTitle(request.getName().trim());
        board.touch();
        boardRepository.save(board);
        return boardDetails(email, boardId);
    }

    @Transactional
    public void delete(String email, Long boardId) {
        Board board = findBoard(boardId);
        permissionService.requireOwner(email, board.getWorkspace().getId());
        boardRepository.delete(board); // Lists/cards cascade through their foreign keys.
    }

    @Transactional
    public BoardSummaryResponse archive(String email, Long boardId) {
        Board board = findBoard(boardId);
        permissionService.requireEditor(email, board.getWorkspace().getId());
        board.setArchived(true);
        board.touch();
        return summary(boardRepository.save(board));
    }

    @Transactional
    public BoardListDetailsResponse createList(String email, Long boardId, CreateBoardListRequest request) {
        Board board = findBoard(boardId);
        permissionService.requireEditor(email, board.getWorkspace().getId());
        double position = listRepository.findMaxPosition(boardId) + 1000.0;
        BoardList list = listRepository.save(new BoardList(board, request.getName().trim(), position));
        return listDetails(list);
    }

    @Transactional
    public BoardListDetailsResponse updateList(String email, Long listId, UpdateBoardListRequest request) {
        BoardList list = findList(listId);
        permissionService.requireEditor(email, list.getBoard().getWorkspace().getId());
        list.setName(request.getName().trim());
        return listDetails(listRepository.save(list));
    }

    @Transactional
    public BoardListDetailsResponse moveList(String email, Long listId, MoveBoardListRequest request) {
        BoardList list = findList(listId);
        permissionService.requireEditor(email, list.getBoard().getWorkspace().getId());
        list.setPosition(request.getPosition());
        List<BoardList> lists = listRepository.findAllByBoard_IdOrderByPositionAsc(list.getBoard().getId());
        lists.sort(Comparator.comparing(BoardList::getPosition).thenComparing(BoardList::getId));
        boolean rebalance = false;
        for (int index = 1; index < lists.size(); index++) {
            if (lists.get(index).getPosition() - lists.get(index - 1).getPosition() < 0.001) {
                rebalance = true;
                break;
            }
        }
        if (rebalance) {
            for (int index = 0; index < lists.size(); index++) {
                lists.get(index).setPosition((index + 1) * 1000.0);
            }
            listRepository.saveAll(lists);
        }
        return listDetails(list);
    }

    @Transactional
    public void deleteList(String email, Long listId) {
        BoardList list = findList(listId);
        permissionService.requireOwner(email, list.getBoard().getWorkspace().getId());
        if (cardRepository.existsByBoardList_Id(listId)) {
            throw new WorkspaceService.WorkspaceConflictException("Move or delete cards before deleting this list");
        }
        listRepository.delete(list);
    }

    private BoardResponse boardDetails(String email, Long boardId) {
        Board board = findBoard(boardId);
        permissionService.requireMember(email, board.getWorkspace().getId());

        // Fixed query count: fetch all lists and all cards in two bulk queries, independent of board size.
        List<BoardList> lists = listRepository.findAllByBoard_IdOrderByPositionAsc(boardId);
        List<Card> cards = cardRepository.findAllByBoardList_Board_IdOrderByPositionAsc(boardId);
        Map<Long, List<CardSummaryResponse>> cardsByList = cards.stream().collect(Collectors.groupingBy(
            card -> card.getBoardList().getId(), Collectors.mapping(this::cardSummary, Collectors.toList())));
        List<BoardListResponse> listResponses = lists.stream()
            .map(list -> new BoardListResponse(list.getId(), list.getName(), list.getPosition(),
                cardsByList.getOrDefault(list.getId(), List.of())))
            .toList();
        return new BoardResponse(board.getId(), board.getWorkspace().getId(), board.getTitle(),
            board.isArchived(), board.getCreatedAt(), listResponses);
    }

    private Board findBoard(Long boardId) {
        return boardRepository.findWithWorkspace(boardId).orElseThrow(BoardNotFoundException::new);
    }

    private BoardList findList(Long listId) {
        return listRepository.findWithBoardById(listId).orElseThrow(BoardNotFoundException::new);
    }

    private BoardSummaryResponse summary(Board board) {
        return new BoardSummaryResponse(board.getId(), board.getWorkspace().getId(), board.getTitle(),
            board.getPosition(), board.isArchived(), board.getCreatedAt());
    }

    private BoardListDetailsResponse listDetails(BoardList list) {
        return new BoardListDetailsResponse(list.getId(), list.getBoard().getId(), list.getName(), list.getPosition());
    }

    private CardSummaryResponse cardSummary(Card card) {
        List<CardAssigneeResponse> assignees = card.getAssignees().stream()
            .map(user -> new CardAssigneeResponse(user.getId(), user.getEmail(), user.getFullName())).toList();
        Long creatorId = card.getCreatedBy() == null ? null : card.getCreatedBy().getId();
        return new CardSummaryResponse(card.getId(), card.getTitle(), card.getDescription(),
            card.getPosition(), card.getDueDate(), card.getPriority(), card.getVersion(), creatorId, assignees);
    }

    public static class BoardNotFoundException extends RuntimeException {
        public BoardNotFoundException() { super("Board or list not found"); }
    }
}
