package com.inote.service;

import com.inote.config.AuthProperties;
import com.inote.model.dto.AuthCaptchaResponse;
import com.inote.model.dto.AuthLoginRequest;
import com.inote.model.dto.AuthResponse;
import com.inote.model.dto.ResetPasswordRequest;
import com.inote.model.entity.User;
import com.inote.repository.UserRepository;
import com.inote.security.CurrentUserService;
import com.inote.security.UnauthorizedException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final String CAPTCHA_SOURCE = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final Duration CAPTCHA_TTL = Duration.ofMinutes(5);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final CurrentUserService currentUserService;
    private final AuthProperties authProperties;
    private final SecureRandom secureRandom = new SecureRandom();
    private final Map<String, CaptchaEntry> captchas = new ConcurrentHashMap<>();

    public AuthCaptchaResponse generateCaptcha() {
        cleanupExpiredCaptchas();
        String captchaId = UUID.randomUUID().toString();
        String captchaCode = randomAlphaNumeric(authProperties.getCaptchaLength());
        captchas.put(captchaId, new CaptchaEntry(captchaCode, LocalDateTime.now().plus(CAPTCHA_TTL)));
        return AuthCaptchaResponse.builder()
                .captchaId(captchaId)
                .captchaCode(captchaCode)
                .build();
    }

    public AuthResponse loginOrRegister(AuthLoginRequest request) {
        validateCaptcha(request.getCaptchaId(), request.getCaptchaCode());

        String username = normalizeUsername(request.getUsername());
        String password = request.getPassword().trim();
        validatePassword(password);

        User user = userRepository.findByUsername(username)
                .map(existing -> validateLogin(existing, password))
                .orElseGet(() -> registerUser(username, password));

        user.setAuthToken(generateAuthToken());
        User saved = userRepository.save(user);
        return AuthResponse.builder()
                .token(saved.getAuthToken())
                .username(saved.getUsername())
                .build();
    }

    public AuthResponse currentUser() {
        User user = currentUserService.getCurrentUser();
        return AuthResponse.builder()
                .token(user.getAuthToken())
                .username(user.getUsername())
                .build();
    }

    public void resetPassword(ResetPasswordRequest request) {
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new IllegalArgumentException("The two password entries do not match.");
        }
        validatePassword(request.getNewPassword());

        User user = currentUserService.getCurrentUser();
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword().trim()));
        user.setAuthToken(generateAuthToken());
        userRepository.save(user);
    }

    private User registerUser(String username, String password) {
        return User.builder()
                .username(username)
                .passwordHash(passwordEncoder.encode(password))
                .authToken(generateAuthToken())
                .build();
    }

    private User validateLogin(User user, String password) {
        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new UnauthorizedException("Username or password is incorrect.");
        }
        return user;
    }

    private void validateCaptcha(String captchaId, String captchaCode) {
        cleanupExpiredCaptchas();
        CaptchaEntry entry = captchas.remove(captchaId);
        if (entry == null || entry.expiresAt().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Captcha has expired. Please refresh and try again.");
        }
        if (!entry.code().equalsIgnoreCase(captchaCode.trim())) {
            throw new IllegalArgumentException("Captcha is incorrect.");
        }
    }

    private void cleanupExpiredCaptchas() {
        LocalDateTime now = LocalDateTime.now();
        captchas.entrySet().removeIf(entry -> entry.getValue().expiresAt().isBefore(now));
    }

    private String normalizeUsername(String username) {
        if (!StringUtils.hasText(username)) {
            throw new IllegalArgumentException("Username must not be blank.");
        }
        String normalized = username.trim().toLowerCase(Locale.ROOT);
        if (!normalized.matches("[a-zA-Z0-9_\\-]{3,32}")) {
            throw new IllegalArgumentException("Username must be 3-32 characters and use letters, numbers, _ or -.");
        }
        return normalized;
    }

    private void validatePassword(String password) {
        if (!StringUtils.hasText(password) || password.trim().length() < 6) {
            throw new IllegalArgumentException("Password must be at least 6 characters.");
        }
    }

    private String randomAlphaNumeric(int length) {
        int actualLength = Math.max(length, 4);
        StringBuilder builder = new StringBuilder(actualLength);
        for (int i = 0; i < actualLength; i++) {
            builder.append(CAPTCHA_SOURCE.charAt(secureRandom.nextInt(CAPTCHA_SOURCE.length())));
        }
        return builder.toString();
    }

    private String generateAuthToken() {
        return UUID.randomUUID().toString().replace("-", "") + UUID.randomUUID().toString().replace("-", "");
    }

    private record CaptchaEntry(String code, LocalDateTime expiresAt) {
    }
}
