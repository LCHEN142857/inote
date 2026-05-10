// 声明当前源文件所属包。
package com.inote.service;

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

// 将当前类注册为服务组件。
@Service
// 让 Lombok 为当前类生成必填依赖构造函数。
@RequiredArgsConstructor
// 定义认证服务，负责验证码校验、登录注册和密码重置逻辑。
public class AuthService {

    // 固定验证码长度为 4 位字母数字组合。
    private static final int CAPTCHA_LENGTH = 4;
    // 计算并保存验证码来源结果。
    private static final String CAPTCHA_SOURCE = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    // 计算并保存验证码ttl结果。
    private static final Duration CAPTCHA_TTL = Duration.ofMinutes(5);

    // 声明用户repository变量，供后续流程使用。
    private final UserRepository userRepository;
    // 声明密码encoder变量，供后续流程使用。
    private final PasswordEncoder passwordEncoder;
    // 声明当前用户service变量，供后续流程使用。
    private final CurrentUserService currentUserService;
    // 声明验证码图片渲染器变量，供后续流程使用。
    private final CaptchaImageRenderer captchaImageRenderer;
    // 声明登录锁定service变量，供后续流程使用。
    private final LoginLockService loginLockService;
    // 创建securerandom对象。
    private final SecureRandom secureRandom = new SecureRandom();
    // 创建captchas对象。
    private final Map<String, CaptchaEntry> captchas = new ConcurrentHashMap<>();

    /**
     * 生成一次性验证码并缓存有效期，供登录前校验使用。
     * @return 认证验证码响应结果。
     */
    public AuthCaptchaResponse generateCaptcha() {
        // 调用 `cleanupExpiredCaptchas` 完成当前步骤。
        cleanupExpiredCaptchas();
        // 生成验证码id随机值。
        String captchaId = UUID.randomUUID().toString();
        // 计算并保存验证码code结果。
        String captchaCode = randomAlphaNumeric(CAPTCHA_LENGTH);
        // 写入当前映射中的键值对。
        captchas.put(captchaId, new CaptchaEntry(captchaCode, LocalDateTime.now().plus(CAPTCHA_TTL)));
        // 将验证码内容渲染为仅供人眼识别的图片。
        String captchaImage = captchaImageRenderer.renderAsDataUri(captchaCode);
        // 返回组装完成的结果对象。
        return AuthCaptchaResponse.builder()
                // 设置验证码id字段的取值。
                .captchaId(captchaId)
                // 设置验证码图片字段的取值。
                .captchaImage(captchaImage)
                // 完成当前建造者对象的组装。
                .build();
    }

    /**
     * 校验验证码后执行登录或自动注册，并刷新用户令牌。
     * @param request 请求参数。
     * @return 认证响应结果。
     */
    public AuthResponse loginOrRegister(AuthLoginRequest request, String clientIp) {
        // 计算并保存username结果。
        String username = normalizeUsername(request.getUsername());
        // 调用 `assertNotLocked` 完成当前步骤。
        loginLockService.assertNotLocked(username, clientIp);

        // 进入异常保护块执行关键逻辑。
        try {
            // 调用 `validateCaptcha` 完成当前步骤。
            validateCaptcha(request.getCaptchaId(), request.getCaptchaCode());

            // 清理并规范化密码内容。
            String password = request.getPassword().trim();
            // 调用 `validatePassword` 完成当前步骤。
            validatePassword(password);

            // 围绕用户用户用户补充当前业务语句。
            User user = userRepository.findByUsername(username)
                    // 设置map字段的取值。
                    .map(existing -> validateLogin(existing, password))
                    // 设置orelseget字段的取值。
                    .orElseGet(() -> registerUser(username, password));

            // 调用 `clear` 完成当前步骤。
            loginLockService.clear(username, clientIp);

            // 更新认证令牌字段。
            user.setAuthToken(generateAuthToken());
            // 保存saved对象。
            User saved = userRepository.save(user);
            // 返回组装完成的结果对象。
            return AuthResponse.builder()
                    // 设置令牌字段的取值。
                    .token(saved.getAuthToken())
                    // 设置username字段的取值。
                    .username(saved.getUsername())
                    // 完成当前建造者对象的组装。
                    .build();
        } catch (IllegalArgumentException | UnauthorizedException ex) {
            // 调用 `recordFailure` 完成当前步骤。
            loginLockService.recordFailure(username, clientIp);
            // 继续抛出原始认证错误。
            throw ex;
        }
    }

    /**
     * 读取当前登录用户并组装认证响应。
     * @return 认证响应结果。
     */
    public AuthResponse currentUser() {
        // 获取当前登录用户。
        User user = currentUserService.getCurrentUser();
        // 返回组装完成的结果对象。
        return AuthResponse.builder()
                // 设置令牌字段的取值。
                .token(user.getAuthToken())
                // 设置username字段的取值。
                .username(user.getUsername())
                // 完成当前建造者对象的组装。
                .build();
    }

    /**
     * 校验新密码后更新当前用户密码并刷新登录令牌。
     * @param request 请求参数。
     */
    public void resetPassword(ResetPasswordRequest request) {
        // 根据条件判断当前分支是否执行。
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            // 抛出 `IllegalArgumentException` 异常中断当前流程。
            throw new IllegalArgumentException("The two password entries do not match.");
        }
        // 调用 `validatePassword` 完成当前步骤。
        validatePassword(request.getNewPassword());

        // 获取当前登录用户。
        User user = currentUserService.getCurrentUser();
        // 更新密码hash字段。
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword().trim()));
        // 更新认证令牌字段。
        user.setAuthToken(generateAuthToken());
        // 保存当前对象到持久化层。
        userRepository.save(user);
    }

    /**
     * 处理注册用户相关逻辑。
     * @param username username参数。
     * @param password 密码参数。
     * @return 用户结果。
     */
    private User registerUser(String username, String password) {
        // 返回组装完成的结果对象。
        return User.builder()
                // 设置username字段的取值。
                .username(username)
                // 设置密码hash字段的取值。
                .passwordHash(passwordEncoder.encode(password))
                // 设置认证令牌字段的取值。
                .authToken(generateAuthToken())
                // 完成当前建造者对象的组装。
                .build();
    }

    /**
     * 处理validate登录相关逻辑。
     * @param user 用户参数。
     * @param password 密码参数。
     * @return 用户结果。
     */
    private User validateLogin(User user, String password) {
        // 根据条件判断当前分支是否执行。
        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            // 抛出 `UnauthorizedException` 异常中断当前流程。
            throw new UnauthorizedException("Username or password is incorrect.");
        }
        // 返回用户。
        return user;
    }

    /**
     * 处理validate验证码相关逻辑。
     * @param captchaId 验证码id参数。
     * @param captchaCode 验证码code参数。
     */
    private void validateCaptcha(String captchaId, String captchaCode) {
        // 调用 `cleanupExpiredCaptchas` 完成当前步骤。
        cleanupExpiredCaptchas();
        // 计算并保存entry结果。
        CaptchaEntry entry = captchas.remove(captchaId);
        // 根据条件判断当前分支是否执行。
        if (entry == null || entry.expiresAt().isBefore(LocalDateTime.now())) {
            // 抛出 `IllegalArgumentException` 异常中断当前流程。
            throw new IllegalArgumentException("Captcha has expired. Please refresh and try again.");
        }
        // 根据条件判断当前分支是否执行。
        if (!entry.code().equalsIgnoreCase(captchaCode.trim())) {
            // 抛出 `IllegalArgumentException` 异常中断当前流程。
            throw new IllegalArgumentException("Captcha is incorrect.");
        }
    }

    /**
     * 处理cleanupexpiredcaptchas相关逻辑。
     */
    private void cleanupExpiredCaptchas() {
        // 计算并保存now结果。
        LocalDateTime now = LocalDateTime.now();
        // 调用 `entrySet` 完成当前步骤。
        captchas.entrySet().removeIf(entry -> entry.getValue().expiresAt().isBefore(now));
    }

    /**
     * 处理normalizeusername相关逻辑。
     * @param username username参数。
     * @return 处理后的字符串结果。
     */
    private String normalizeUsername(String username) {
        // 根据条件判断当前分支是否执行。
        if (!StringUtils.hasText(username)) {
            // 抛出 `IllegalArgumentException` 异常中断当前流程。
            throw new IllegalArgumentException("Username must not be blank.");
        }
        // 清理并规范化normalized内容。
        String normalized = username.trim().toLowerCase(Locale.ROOT);
        // 根据条件判断当前分支是否执行。
        if (!normalized.matches("[a-zA-Z0-9_\\-]{3,32}")) {
            // 抛出 `IllegalArgumentException` 异常中断当前流程。
            throw new IllegalArgumentException("Username must be 3-32 characters and use letters, numbers, _ or -.");
        }
        // 返回normalized。
        return normalized;
    }

    /**
     * 处理validate密码相关逻辑。
     * @param password 密码参数。
     */
    private void validatePassword(String password) {
        // 根据条件判断当前分支是否执行。
        if (!StringUtils.hasText(password) || password.trim().length() < 6) {
            // 抛出 `IllegalArgumentException` 异常中断当前流程。
            throw new IllegalArgumentException("Password must be at least 6 characters.");
        }
    }

    /**
     * 处理randomalphanumeric相关逻辑。
     * @param length length参数。
     * @return 处理后的字符串结果。
     */
    private String randomAlphaNumeric(int length) {
        // 计算并保存actuallength结果。
        int actualLength = Math.max(length, 4);
        // 创建builder对象。
        StringBuilder builder = new StringBuilder(actualLength);
        // 遍历当前集合或区间中的元素。
        for (int i = 0; i < actualLength; i++) {
            // 调用 `append` 完成当前步骤。
            builder.append(CAPTCHA_SOURCE.charAt(secureRandom.nextInt(CAPTCHA_SOURCE.length())));
        }
        // 返回 `toString` 的处理结果。
        return builder.toString();
    }

    /**
     * 处理generate认证令牌相关逻辑。
     * @return 处理后的字符串结果。
     */
    private String generateAuthToken() {
        // 返回 `randomUUID` 的处理结果。
        return UUID.randomUUID().toString().replace("-", "") + UUID.randomUUID().toString().replace("-", "");
    }

    /**
     * 处理验证码entry相关逻辑。
     * @param code code参数。
     * @param expiresAt expiresat参数。
     * @return record结果。
     */
    private record CaptchaEntry(String code, LocalDateTime expiresAt) {
    }
}
