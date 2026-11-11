package com.syncflow.backend.controller;

import com.syncflow.backend.service.AuthService;
import com.syncflow.backend.service.RefreshTokenService;
import com.syncflow.backend.service.WorkspaceService;
import com.syncflow.backend.service.BoardService;
import com.syncflow.backend.service.CardService;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;

@RestControllerAdvice
public class ApiExceptionHandler {
    @ExceptionHandler(AuthService.InvalidCredentialsException.class)
    public ResponseEntity<ProblemDetail> invalidCredentials() {
        return problem(HttpStatus.UNAUTHORIZED, "Email hoặc mật khẩu không đúng");
    }

    @ExceptionHandler(RefreshTokenService.InvalidRefreshTokenException.class)
    public ResponseEntity<ProblemDetail> invalidRefreshToken() {
        return problem(HttpStatus.UNAUTHORIZED, "Refresh token không hợp lệ hoặc đã hết hạn");
    }

    @ExceptionHandler(AuthService.EmailAlreadyExistsException.class)
    public ResponseEntity<ProblemDetail> duplicateEmail() {
        return problem(HttpStatus.CONFLICT, "Email đã được sử dụng");
    }

    @ExceptionHandler(AuthService.UserNotFoundException.class)
    public ResponseEntity<ProblemDetail> userNotFound() {
        return problem(HttpStatus.NOT_FOUND, "Không tìm thấy người dùng");
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ProblemDetail> accessDenied() {
        return problem(HttpStatus.FORBIDDEN, "Bạn không có quyền thực hiện thao tác này");
    }

    @ExceptionHandler(WorkspaceService.WorkspaceNotFoundException.class)
    public ResponseEntity<ProblemDetail> workspaceNotFound() {
        return problem(HttpStatus.NOT_FOUND, "Workspace hoặc membership không tồn tại");
    }

    @ExceptionHandler(BoardService.BoardNotFoundException.class)
    public ResponseEntity<ProblemDetail> boardNotFound() {
        return problem(HttpStatus.NOT_FOUND, "Board hoặc list không tồn tại");
    }

    @ExceptionHandler(CardService.CardNotFoundException.class)
    public ResponseEntity<ProblemDetail> cardNotFound() {
        return problem(HttpStatus.NOT_FOUND, "Card không tồn tại");
    }

    @ExceptionHandler({CardService.CardConflictException.class, OptimisticLockingFailureException.class})
    public ResponseEntity<ProblemDetail> cardConflict(Exception exception) {
        return problem(HttpStatus.CONFLICT, exception.getMessage());
    }

    @ExceptionHandler(CardService.InvalidCardRelationException.class)
    public ResponseEntity<ProblemDetail> invalidCardRelation(CardService.InvalidCardRelationException exception) {
        return problem(HttpStatus.UNPROCESSABLE_ENTITY, exception.getMessage());
    }

    @ExceptionHandler(WorkspaceService.InvalidInvitationException.class)
    public ResponseEntity<ProblemDetail> invalidInvitation() {
        return problem(HttpStatus.NOT_FOUND, "Invitation không hợp lệ, hết hạn hoặc đã được sử dụng");
    }

    @ExceptionHandler(WorkspaceService.WorkspaceConflictException.class)
    public ResponseEntity<ProblemDetail> workspaceConflict(WorkspaceService.WorkspaceConflictException exception) {
        return problem(HttpStatus.CONFLICT, exception.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ProblemDetail> invalidRequest(IllegalArgumentException exception) {
        return problem(HttpStatus.BAD_REQUEST, exception.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> invalidBody(MethodArgumentNotValidException exception) {
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Dữ liệu gửi lên không hợp lệ");
        detail.setTitle("Validation failed");
        var errors = new LinkedHashMap<String, String>();
        exception.getBindingResult().getFieldErrors().forEach(error ->
            errors.putIfAbsent(error.getField(), error.getDefaultMessage()));
        detail.setProperty("errors", errors);
        return ResponseEntity.badRequest().body(detail);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ProblemDetail> unreadableBody() {
        return problem(HttpStatus.BAD_REQUEST, "Request body không hợp lệ");
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ProblemDetail> dataConflict() {
        return problem(HttpStatus.CONFLICT, "Dữ liệu xung đột với tài khoản hiện có");
    }

    private ResponseEntity<ProblemDetail> problem(HttpStatus status, String message) {
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(status, message);
        detail.setTitle(status.getReasonPhrase());
        return ResponseEntity.status(status).body(detail);
    }
}
