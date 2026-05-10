// 声明当前源文件所属包。
package com.inote.controller;

import com.inote.model.dto.AuthCaptchaResponse;
import com.inote.model.dto.AuthLoginRequest;
import com.inote.model.dto.AuthResponse;
import com.inote.model.dto.ResetPasswordRequest;
import com.inote.security.ClientIpResolver;
import com.inote.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

// 声明当前类提供 REST 风格接口。
@RestController
// 声明当前控制器的统一请求路径前缀。
@RequestMapping("/api/v1/auth")
// 让 Lombok 为当前类生成必填依赖构造函数。
@RequiredArgsConstructor
// 定义认证接口控制器，负责验证码、登录态和密码重置接口。
public class AuthController {

    // 声明认证service变量，供后续流程使用。
    private final AuthService authService;

    /**
     * 返回登录前使用的验证码信息。
     * @return 封装后的 HTTP 响应结果。
     */
    // 声明当前方法处理 GET 请求。
    @GetMapping("/captcha")
    public ResponseEntity<AuthCaptchaResponse> captcha() {
        // 返回成功响应。
        return ResponseEntity.ok(authService.generateCaptcha());
    }

    /**
     * 处理用户名密码登录请求，并在首次登录时自动注册。
     * @param request 请求参数。
     * @return 封装后的 HTTP 响应结果。
     */
    // 声明当前方法处理 POST 请求。
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody AuthLoginRequest request, HttpServletRequest httpRequest) {
        // 返回成功响应。
        return ResponseEntity.ok(authService.loginOrRegister(request, ClientIpResolver.resolve(httpRequest)));
    }

    /**
     * 返回当前登录用户的认证信息。
     * @return 封装后的 HTTP 响应结果。
     */
    // 声明当前方法处理 GET 请求。
    @GetMapping("/me")
    public ResponseEntity<AuthResponse> me() {
        // 返回成功响应。
        return ResponseEntity.ok(authService.currentUser());
    }

    /**
     * 处理当前用户的密码重置请求。
     * @param request 请求参数。
     * @return string>>结果。
     */
    // 声明当前方法处理 POST 请求。
    @PostMapping("/password/reset")
    public ResponseEntity<Map<String, String>> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        // 调用 `resetPassword` 完成当前步骤。
        authService.resetPassword(request);
        // 返回成功响应。
        return ResponseEntity.ok(Map.of("message", "Password reset successful. Please use the new password next time."));
    }

}
