package com.syncflow.backend.service;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import com.syncflow.backend.dto.RegisterRequest;
import com.syncflow.backend.dto.LoginRequest;
import com.syncflow.backend.dto.AuthResponse;
import com.syncflow.backend.dto.RefreshRequest;
import com.syncflow.backend.model.User;
import com.syncflow.backend.repository.UserRepository;

@Service 
public class AuthService {
    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;

    public AuthService(UserRepository userRepository, BCryptPasswordEncoder passwordEncoder,
                       JwtService jwtService, RefreshTokenService refreshTokenService){
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
    }

    public String register(RegisterRequest request){
        if(userRepository.existsByEmail(request.getEmail())){
            throw new IllegalArgumentException("Email already exists");
        }

        String encodedPassword = passwordEncoder.encode(request.getPassword());

        User newUser = new User(request.getEmail(), encodedPassword, request.getFullName());

        userRepository.save(newUser);

        return "Register successfully";
    }

    public AuthResponse login(LoginRequest request) {
        if (request == null || request.getEmail() == null || request.getPassword() == null) {
            throw new InvalidCredentialsException();
        }
        User user = userRepository.findByEmail(request.getEmail())
            .filter(candidate -> passwordEncoder.matches(request.getPassword(), candidate.getPasswordHash()))
            .orElseThrow(() -> new InvalidCredentialsException());

        return issueTokens(user);
    }

    public AuthResponse refresh(RefreshRequest request) {
        User user = refreshTokenService.rotate(request == null ? null : request.getRefreshToken());
        return issueTokens(user);
    }

    private AuthResponse issueTokens(User user) {
        String accessToken = jwtService.createAccessToken(user);
        String refreshToken = refreshTokenService.create(user);
        return new AuthResponse(user.getId(), user.getEmail(), user.getFullName(),
            accessToken, refreshToken, jwtService.getExpirationMillis() / 1000);
    }

    public static class InvalidCredentialsException extends RuntimeException {
        public InvalidCredentialsException() {
            super("Invalid email or password");
        }
    }
}
