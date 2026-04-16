// 声明当前源文件的包。
package com.inote.controller;

import com.inote.model.dto.AuthCaptchaResponse;
import com.inote.model.dto.AuthLoginRequest;
import com.inote.model.dto.AuthResponse;
import com.inote.model.dto.ResetPasswordRequest;
import com.inote.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

// 应用当前注解。
@RestController
// 应用当前注解。
@RequestMapping("/api/v1/auth")
// 应用当前注解。
@RequiredArgsConstructor
// 声明当前类型。
public class AuthController {

    // 声明当前字段。
    private final AuthService authService;

    /**
     * 描述 `captcha` 操作。
     *
     * @return 类型为 `ResponseEntity<AuthCaptchaResponse>` 的返回值。
     */
    // 应用当前注解。
    @GetMapping("/captcha")
    // 处理当前代码结构。
    public ResponseEntity<AuthCaptchaResponse> captcha() {
        // 返回当前结果。
        return ResponseEntity.ok(authService.generateCaptcha());
    // 结束当前代码块。
    }

    /**
     * 描述 `login` 操作。
     *
     * @param request 输入参数 `request`。
     * @return 类型为 `ResponseEntity<AuthResponse>` 的返回值。
     */
    // 应用当前注解。
    @PostMapping("/login")
    // 处理当前代码结构。
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody AuthLoginRequest request) {
        // 返回当前结果。
        return ResponseEntity.ok(authService.loginOrRegister(request));
    // 结束当前代码块。
    }

    /**
     * 描述 `me` 操作。
     *
     * @return 类型为 `ResponseEntity<AuthResponse>` 的返回值。
     */
    // 应用当前注解。
    @GetMapping("/me")
    // 处理当前代码结构。
    public ResponseEntity<AuthResponse> me() {
        // 返回当前结果。
        return ResponseEntity.ok(authService.currentUser());
    // 结束当前代码块。
    }

    /**
     * 描述 `resetPassword` 操作。
     *
     * @param request 输入参数 `request`。
     * @return 类型为 `ResponseEntity<Map<String, String>>` 的返回值。
     */
    // 应用当前注解。
    @PostMapping("/password/reset")
    // 处理当前代码结构。
    public ResponseEntity<Map<String, String>> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        // 执行当前语句。
        authService.resetPassword(request);
        // 返回当前结果。
        return ResponseEntity.ok(Map.of("message", "Password reset successful. Please use the new password next time."));
    // 结束当前代码块。
    }
// 结束当前代码块。
}
