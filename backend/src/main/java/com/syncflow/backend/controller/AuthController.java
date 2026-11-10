package com.syncflow.backend.controller;

import com.syncflow.backend.dto.RegisterRequest;
import com.syncflow.backend.dto.LoginRequest;
import com.syncflow.backend.dto.AuthResponse;
import com.syncflow.backend.dto.RefreshRequest;
import com.syncflow.backend.service.AuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService){
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<String> register(@RequestBody RegisterRequest request){
        String responseMessage = authService.register(request);
        return ResponseEntity.ok(responseMessage);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@RequestBody RefreshRequest request) {
        return ResponseEntity.ok(authService.refresh(request));
    }

    @ExceptionHandler(AuthService.InvalidCredentialsException.class)
    public ResponseEntity<String> invalidCredentials(AuthService.InvalidCredentialsException exception) {
        return ResponseEntity.status(401).body(exception.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> badRequest(IllegalArgumentException exception) {
        return ResponseEntity.badRequest().body(exception.getMessage());
    }

    @ExceptionHandler(com.syncflow.backend.service.RefreshTokenService.InvalidRefreshTokenException.class)
    public ResponseEntity<String> invalidRefreshToken(RuntimeException exception) {
        return ResponseEntity.status(401).body(exception.getMessage());
    }
}
