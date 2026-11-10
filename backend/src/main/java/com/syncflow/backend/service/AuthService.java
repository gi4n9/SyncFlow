package com.syncflow.backend.service;

import com.syncflow.backend.dto.*;
import com.syncflow.backend.model.User;
import com.syncflow.backend.repository.UserRepository;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class AuthService {
    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);
    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;

    public AuthService(UserRepository userRepository, BCryptPasswordEncoder passwordEncoder,
                       JwtService jwtService, RefreshTokenService refreshTokenService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
    }

    @Transactional
    public UserProfileResponse register(RegisterRequest request) {
        String email = normalizeEmail(request.getEmail());
        if (userRepository.existsByEmail(email)) throw new EmailAlreadyExistsException();

        User user = new User(email, passwordEncoder.encode(request.getPassword()), request.getFullName().trim());
        try {
            userRepository.saveAndFlush(user);
        } catch (org.springframework.dao.DataIntegrityViolationException exception) {
            throw new EmailAlreadyExistsException();
        }
        return profile(user);
    }

    public TokenSession login(LoginRequest request) {
        if (request == null || request.getEmail() == null || request.getPassword() == null) {
            throw new InvalidCredentialsException();
        }
        User user = userRepository.findByEmail(normalizeEmail(request.getEmail()))
            .filter(candidate -> passwordEncoder.matches(request.getPassword(), candidate.getPasswordHash()))
            .orElseThrow(() -> {
                logger.warn("Failed login attempt");
                return new InvalidCredentialsException();
            });
        return issueTokens(user);
    }

    public TokenSession refresh(String rawRefreshToken) {
        RefreshTokenService.RotatedToken rotated = refreshTokenService.rotate(rawRefreshToken);
        return new TokenSession(accessResponse(rotated.user()), rotated.refreshToken());
    }

    public void logout(String rawRefreshToken) {
        refreshTokenService.revoke(rawRefreshToken);
    }

    public UserProfileResponse getProfile(String currentEmail) {
        return profile(findCurrentUser(currentEmail));
    }

    @Transactional
    public UserProfileResponse updateProfile(String currentEmail, UpdateProfileRequest request) {
        User user = findCurrentUser(currentEmail);
        String email = normalizeEmail(request.getEmail());
        if (!user.getEmail().equals(email) && userRepository.existsByEmailAndIdNot(email, user.getId())) {
            throw new EmailAlreadyExistsException();
        }
        user.setEmail(email);
        user.setFullName(request.getFullName().trim());
        return profile(userRepository.save(user));
    }

    @Transactional
    public void changePassword(String currentEmail, ChangePasswordRequest request) {
        User user = findCurrentUser(currentEmail);
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
        refreshTokenService.revokeAllForUser(user.getId());
    }

    private TokenSession issueTokens(User user) {
        return new TokenSession(accessResponse(user), refreshTokenService.create(user));
    }

    private AuthResponse accessResponse(User user) {
        return new AuthResponse(user.getId(), user.getEmail(), user.getFullName(),
            jwtService.createAccessToken(user), jwtService.getExpirationMillis() / 1000);
    }

    private User findCurrentUser(String email) {
        return userRepository.findByEmail(email).orElseThrow(UserNotFoundException::new);
    }

    private UserProfileResponse profile(User user) {
        return new UserProfileResponse(user.getId(), user.getEmail(), user.getFullName());
    }

    private String normalizeEmail(String email) {
        if (email == null) throw new IllegalArgumentException("Email is required");
        return email.trim().toLowerCase(Locale.ROOT);
    }

    public static class InvalidCredentialsException extends RuntimeException {
        public InvalidCredentialsException() { super("Email or password is incorrect"); }
    }

    public static class EmailAlreadyExistsException extends RuntimeException {
        public EmailAlreadyExistsException() { super("An account with this email already exists"); }
    }

    public static class UserNotFoundException extends RuntimeException {
        public UserNotFoundException() { super("User not found"); }
    }
}
