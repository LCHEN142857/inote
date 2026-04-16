// 声明当前源文件的包。
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

// 应用当前注解。
@Service
// 应用当前注解。
@RequiredArgsConstructor
// 声明当前类型。
public class AuthService {

    // 声明当前字段。
    private static final String CAPTCHA_SOURCE = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    /**
     * 描述 `Duration.ofMinutes` 操作。
     *
     * @param 5 输入参数 `5`。
     * @return 类型为 `Duration CAPTCHA_TTL =` 的返回值。
     */
    // 执行当前语句。
    private static final Duration CAPTCHA_TTL = Duration.ofMinutes(5);

    // 声明当前字段。
    private final UserRepository userRepository;
    // 声明当前字段。
    private final PasswordEncoder passwordEncoder;
    // 声明当前字段。
    private final CurrentUserService currentUserService;
    // 声明当前字段。
    private final AuthProperties authProperties;
    /**
     * 描述 `SecureRandom` 操作。
     *
     * @return 类型为 `SecureRandom secureRandom = new` 的返回值。
     */
    // 执行当前语句。
    private final SecureRandom secureRandom = new SecureRandom();
    /**
     * 描述 `ConcurrentHashMap<>` 操作。
     *
     * @return 类型为 `Map<String, CaptchaEntry> captchas = new` 的返回值。
     */
    // 执行当前语句。
    private final Map<String, CaptchaEntry> captchas = new ConcurrentHashMap<>();

    /**
     * 描述 `generateCaptcha` 操作。
     *
     * @return 类型为 `AuthCaptchaResponse` 的返回值。
     */
    // 处理当前代码结构。
    public AuthCaptchaResponse generateCaptcha() {
        // 执行当前语句。
        cleanupExpiredCaptchas();
        // 执行当前语句。
        String captchaId = UUID.randomUUID().toString();
        // 执行当前语句。
        String captchaCode = randomAlphaNumeric(authProperties.getCaptchaLength());
        // 执行当前语句。
        captchas.put(captchaId, new CaptchaEntry(captchaCode, LocalDateTime.now().plus(CAPTCHA_TTL)));
        // 返回当前结果。
        return AuthCaptchaResponse.builder()
                // 处理当前代码结构。
                .captchaId(captchaId)
                // 处理当前代码结构。
                .captchaCode(captchaCode)
                // 执行当前语句。
                .build();
    // 结束当前代码块。
    }

    /**
     * 描述 `loginOrRegister` 操作。
     *
     * @param request 输入参数 `request`。
     * @return 类型为 `AuthResponse` 的返回值。
     */
    // 处理当前代码结构。
    public AuthResponse loginOrRegister(AuthLoginRequest request) {
        // 执行当前语句。
        validateCaptcha(request.getCaptchaId(), request.getCaptchaCode());

        // 执行当前语句。
        String username = normalizeUsername(request.getUsername());
        // 执行当前语句。
        String password = request.getPassword().trim();
        // 执行当前语句。
        validatePassword(password);

        // 处理当前代码结构。
        User user = userRepository.findByUsername(username)
                // 处理当前代码结构。
                .map(existing -> validateLogin(existing, password))
                // 执行当前语句。
                .orElseGet(() -> registerUser(username, password));

        // 执行当前语句。
        user.setAuthToken(generateAuthToken());
        // 执行当前语句。
        User saved = userRepository.save(user);
        // 返回当前结果。
        return AuthResponse.builder()
                // 处理当前代码结构。
                .token(saved.getAuthToken())
                // 处理当前代码结构。
                .username(saved.getUsername())
                // 执行当前语句。
                .build();
    // 结束当前代码块。
    }

    /**
     * 描述 `currentUser` 操作。
     *
     * @return 类型为 `AuthResponse` 的返回值。
     */
    // 处理当前代码结构。
    public AuthResponse currentUser() {
        // 执行当前语句。
        User user = currentUserService.getCurrentUser();
        // 返回当前结果。
        return AuthResponse.builder()
                // 处理当前代码结构。
                .token(user.getAuthToken())
                // 处理当前代码结构。
                .username(user.getUsername())
                // 执行当前语句。
                .build();
    // 结束当前代码块。
    }

    /**
     * 描述 `resetPassword` 操作。
     *
     * @param request 输入参数 `request`。
     * @return 无返回值。
     */
    // 处理当前代码结构。
    public void resetPassword(ResetPasswordRequest request) {
        // 执行当前流程控制分支。
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            // 抛出当前异常。
            throw new IllegalArgumentException("The two password entries do not match.");
        // 结束当前代码块。
        }
        // 执行当前语句。
        validatePassword(request.getNewPassword());

        // 执行当前语句。
        User user = currentUserService.getCurrentUser();
        // 执行当前语句。
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword().trim()));
        // 执行当前语句。
        user.setAuthToken(generateAuthToken());
        // 执行当前语句。
        userRepository.save(user);
    // 结束当前代码块。
    }

    /**
     * 描述 `registerUser` 操作。
     *
     * @param username 输入参数 `username`。
     * @param password 输入参数 `password`。
     * @return 类型为 `User` 的返回值。
     */
    // 处理当前代码结构。
    private User registerUser(String username, String password) {
        // 返回当前结果。
        return User.builder()
                // 处理当前代码结构。
                .username(username)
                // 处理当前代码结构。
                .passwordHash(passwordEncoder.encode(password))
                // 处理当前代码结构。
                .authToken(generateAuthToken())
                // 执行当前语句。
                .build();
    // 结束当前代码块。
    }

    /**
     * 描述 `validateLogin` 操作。
     *
     * @param user 输入参数 `user`。
     * @param password 输入参数 `password`。
     * @return 类型为 `User` 的返回值。
     */
    // 处理当前代码结构。
    private User validateLogin(User user, String password) {
        // 执行当前流程控制分支。
        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            // 抛出当前异常。
            throw new UnauthorizedException("Username or password is incorrect.");
        // 结束当前代码块。
        }
        // 返回当前结果。
        return user;
    // 结束当前代码块。
    }

    /**
     * 描述 `validateCaptcha` 操作。
     *
     * @param captchaId 输入参数 `captchaId`。
     * @param captchaCode 输入参数 `captchaCode`。
     * @return 无返回值。
     */
    // 处理当前代码结构。
    private void validateCaptcha(String captchaId, String captchaCode) {
        // 执行当前语句。
        cleanupExpiredCaptchas();
        // 执行当前语句。
        CaptchaEntry entry = captchas.remove(captchaId);
        // 执行当前流程控制分支。
        if (entry == null || entry.expiresAt().isBefore(LocalDateTime.now())) {
            // 抛出当前异常。
            throw new IllegalArgumentException("Captcha has expired. Please refresh and try again.");
        // 结束当前代码块。
        }
        // 执行当前流程控制分支。
        if (!entry.code().equalsIgnoreCase(captchaCode.trim())) {
            // 抛出当前异常。
            throw new IllegalArgumentException("Captcha is incorrect.");
        // 结束当前代码块。
        }
    // 结束当前代码块。
    }

    /**
     * 描述 `cleanupExpiredCaptchas` 操作。
     *
     * @return 无返回值。
     */
    // 处理当前代码结构。
    private void cleanupExpiredCaptchas() {
        // 执行当前语句。
        LocalDateTime now = LocalDateTime.now();
        // 执行当前语句。
        captchas.entrySet().removeIf(entry -> entry.getValue().expiresAt().isBefore(now));
    // 结束当前代码块。
    }

    /**
     * 描述 `normalizeUsername` 操作。
     *
     * @param username 输入参数 `username`。
     * @return 类型为 `String` 的返回值。
     */
    // 处理当前代码结构。
    private String normalizeUsername(String username) {
        // 执行当前流程控制分支。
        if (!StringUtils.hasText(username)) {
            // 抛出当前异常。
            throw new IllegalArgumentException("Username must not be blank.");
        // 结束当前代码块。
        }
        // 执行当前语句。
        String normalized = username.trim().toLowerCase(Locale.ROOT);
        // 执行当前流程控制分支。
        if (!normalized.matches("[a-zA-Z0-9_\\-]{3,32}")) {
            // 抛出当前异常。
            throw new IllegalArgumentException("Username must be 3-32 characters and use letters, numbers, _ or -.");
        // 结束当前代码块。
        }
        // 返回当前结果。
        return normalized;
    // 结束当前代码块。
    }

    /**
     * 描述 `validatePassword` 操作。
     *
     * @param password 输入参数 `password`。
     * @return 无返回值。
     */
    // 处理当前代码结构。
    private void validatePassword(String password) {
        // 执行当前流程控制分支。
        if (!StringUtils.hasText(password) || password.trim().length() < 6) {
            // 抛出当前异常。
            throw new IllegalArgumentException("Password must be at least 6 characters.");
        // 结束当前代码块。
        }
    // 结束当前代码块。
    }

    /**
     * 描述 `randomAlphaNumeric` 操作。
     *
     * @param length 输入参数 `length`。
     * @return 类型为 `String` 的返回值。
     */
    // 处理当前代码结构。
    private String randomAlphaNumeric(int length) {
        // 执行当前语句。
        int actualLength = Math.max(length, 4);
        // 执行当前语句。
        StringBuilder builder = new StringBuilder(actualLength);
        // 执行当前流程控制分支。
        for (int i = 0; i < actualLength; i++) {
            // 执行当前语句。
            builder.append(CAPTCHA_SOURCE.charAt(secureRandom.nextInt(CAPTCHA_SOURCE.length())));
        // 结束当前代码块。
        }
        // 返回当前结果。
        return builder.toString();
    // 结束当前代码块。
    }

    /**
     * 描述 `generateAuthToken` 操作。
     *
     * @return 类型为 `String` 的返回值。
     */
    // 处理当前代码结构。
    private String generateAuthToken() {
        // 返回当前结果。
        return UUID.randomUUID().toString().replace("-", "") + UUID.randomUUID().toString().replace("-", "");
    // 结束当前代码块。
    }

    /**
     * 描述 `CaptchaEntry` 操作。
     *
     * @param code 输入参数 `code`。
     * @param expiresAt 输入参数 `expiresAt`。
     * @return 类型为 `record` 的返回值。
     */
    // 声明当前类型。
    private record CaptchaEntry(String code, LocalDateTime expiresAt) {
    // 结束当前代码块。
    }
// 结束当前代码块。
}
