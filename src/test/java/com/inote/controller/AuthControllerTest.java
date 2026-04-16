// 声明当前源文件的包。
package com.inote.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.inote.model.dto.AuthCaptchaResponse;
import com.inote.model.dto.AuthLoginRequest;
import com.inote.model.dto.AuthResponse;
import com.inote.model.dto.ResetPasswordRequest;
import com.inote.repository.UserRepository;
import com.inote.security.UnauthorizedException;
import com.inote.service.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// 应用当前注解。
@WebMvcTest(AuthController.class)
// 应用当前注解。
@AutoConfigureMockMvc(addFilters = false)
// 声明当前类型。
class AuthControllerTest {

    // 应用当前注解。
    @Autowired
    // 声明当前字段。
    private MockMvc mockMvc;

    // 应用当前注解。
    @Autowired
    // 声明当前字段。
    private ObjectMapper objectMapper;

    // 应用当前注解。
    @MockBean
    // 声明当前字段。
    private AuthService authService;

    // 应用当前注解。
    @MockBean
    // 声明当前字段。
    private UserRepository userRepository;

    /**
     * 描述 `captchaReturnsCaptchaPayload` 操作。
     *
     * @return 无返回值。
     * @throws Exception 已声明的异常类型 `Exception`。
     */
    // 应用当前注解。
    @Test
    // 处理当前代码结构。
    void captchaReturnsCaptchaPayload() throws Exception {
        // 处理当前代码结构。
        when(authService.generateCaptcha()).thenReturn(AuthCaptchaResponse.builder()
                // 处理当前代码结构。
                .captchaId("captcha-1")
                // 处理当前代码结构。
                .captchaCode("ABCD")
                // 执行当前语句。
                .build());

        // 处理当前代码结构。
        mockMvc.perform(get("/api/v1/auth/captcha"))
                // 处理当前代码结构。
                .andExpect(status().isOk())
                // 处理当前代码结构。
                .andExpect(jsonPath("$.captchaId").value("captcha-1"))
                // 执行当前语句。
                .andExpect(jsonPath("$.captchaCode").value("ABCD"));
    // 结束当前代码块。
    }

    /**
     * 描述 `loginReturnsAuthResponseWhenRequestIsValid` 操作。
     *
     * @return 无返回值。
     * @throws Exception 已声明的异常类型 `Exception`。
     */
    // 应用当前注解。
    @Test
    // 处理当前代码结构。
    void loginReturnsAuthResponseWhenRequestIsValid() throws Exception {
        // 处理当前代码结构。
        when(authService.loginOrRegister(any(AuthLoginRequest.class))).thenReturn(AuthResponse.builder()
                // 处理当前代码结构。
                .token("token-1")
                // 处理当前代码结构。
                .username("tester")
                // 执行当前语句。
                .build());

        // 处理当前代码结构。
        mockMvc.perform(post("/api/v1/auth/login")
                        // 处理当前代码结构。
                        .contentType(MediaType.APPLICATION_JSON)
                        // 处理当前代码结构。
                        .content(objectMapper.writeValueAsString(Map.of(
                                // 处理当前代码结构。
                                "username", "tester",
                                // 处理当前代码结构。
                                "password", "secret123",
                                // 处理当前代码结构。
                                "captchaId", "captcha-1",
                                // 处理当前代码结构。
                                "captchaCode", "ABCD"))))
                // 处理当前代码结构。
                .andExpect(status().isOk())
                // 处理当前代码结构。
                .andExpect(jsonPath("$.token").value("token-1"))
                // 执行当前语句。
                .andExpect(jsonPath("$.username").value("tester"));
    // 结束当前代码块。
    }

    /**
     * 描述 `loginReturnsBadRequestWhenValidationFails` 操作。
     *
     * @return 无返回值。
     * @throws Exception 已声明的异常类型 `Exception`。
     */
    // 应用当前注解。
    @Test
    // 处理当前代码结构。
    void loginReturnsBadRequestWhenValidationFails() throws Exception {
        // 处理当前代码结构。
        mockMvc.perform(post("/api/v1/auth/login")
                        // 处理当前代码结构。
                        .contentType(MediaType.APPLICATION_JSON)
                        // 处理当前代码结构。
                        .content(objectMapper.writeValueAsString(Map.of(
                                // 处理当前代码结构。
                                "username", "",
                                // 处理当前代码结构。
                                "password", "secret123",
                                // 处理当前代码结构。
                                "captchaId", "captcha-1",
                                // 处理当前代码结构。
                                "captchaCode", "ABCD"))))
                // 处理当前代码结构。
                .andExpect(status().isBadRequest())
                // 执行当前语句。
                .andExpect(jsonPath("$.username").value("username must not be blank"));
    // 结束当前代码块。
    }

    /**
     * 描述 `loginReturnsUnauthorizedWhenServiceRejectsCredentials` 操作。
     *
     * @return 无返回值。
     * @throws Exception 已声明的异常类型 `Exception`。
     */
    // 应用当前注解。
    @Test
    // 处理当前代码结构。
    void loginReturnsUnauthorizedWhenServiceRejectsCredentials() throws Exception {
        // 处理当前代码结构。
        when(authService.loginOrRegister(any(AuthLoginRequest.class)))
                // 执行当前语句。
                .thenThrow(new UnauthorizedException("Username or password is incorrect."));

        // 处理当前代码结构。
        mockMvc.perform(post("/api/v1/auth/login")
                        // 处理当前代码结构。
                        .contentType(MediaType.APPLICATION_JSON)
                        // 处理当前代码结构。
                        .content(objectMapper.writeValueAsString(Map.of(
                                // 处理当前代码结构。
                                "username", "tester",
                                // 处理当前代码结构。
                                "password", "secret123",
                                // 处理当前代码结构。
                                "captchaId", "captcha-1",
                                // 处理当前代码结构。
                                "captchaCode", "ABCD"))))
                // 处理当前代码结构。
                .andExpect(status().isUnauthorized())
                // 执行当前语句。
                .andExpect(jsonPath("$.error").value("Username or password is incorrect."));
    // 结束当前代码块。
    }

    /**
     * 描述 `meReturnsCurrentUser` 操作。
     *
     * @return 无返回值。
     * @throws Exception 已声明的异常类型 `Exception`。
     */
    // 应用当前注解。
    @Test
    // 处理当前代码结构。
    void meReturnsCurrentUser() throws Exception {
        // 处理当前代码结构。
        when(authService.currentUser()).thenReturn(AuthResponse.builder()
                // 处理当前代码结构。
                .token("token-2")
                // 处理当前代码结构。
                .username("current-user")
                // 执行当前语句。
                .build());

        // 处理当前代码结构。
        mockMvc.perform(get("/api/v1/auth/me"))
                // 处理当前代码结构。
                .andExpect(status().isOk())
                // 处理当前代码结构。
                .andExpect(jsonPath("$.token").value("token-2"))
                // 执行当前语句。
                .andExpect(jsonPath("$.username").value("current-user"));
    // 结束当前代码块。
    }

    /**
     * 描述 `meReturnsUnauthorizedWhenNoCurrentUserExists` 操作。
     *
     * @return 无返回值。
     * @throws Exception 已声明的异常类型 `Exception`。
     */
    // 应用当前注解。
    @Test
    // 处理当前代码结构。
    void meReturnsUnauthorizedWhenNoCurrentUserExists() throws Exception {
        // 执行当前语句。
        when(authService.currentUser()).thenThrow(new UnauthorizedException("Authentication required."));

        // 处理当前代码结构。
        mockMvc.perform(get("/api/v1/auth/me"))
                // 处理当前代码结构。
                .andExpect(status().isUnauthorized())
                // 执行当前语句。
                .andExpect(jsonPath("$.error").value("Authentication required."));
    // 结束当前代码块。
    }

    /**
     * 描述 `resetPasswordReturnsSuccessMessageWhenRequestIsValid` 操作。
     *
     * @return 无返回值。
     * @throws Exception 已声明的异常类型 `Exception`。
     */
    // 应用当前注解。
    @Test
    // 处理当前代码结构。
    void resetPasswordReturnsSuccessMessageWhenRequestIsValid() throws Exception {
        // 执行当前语句。
        doNothing().when(authService).resetPassword(any(ResetPasswordRequest.class));

        // 处理当前代码结构。
        mockMvc.perform(post("/api/v1/auth/password/reset")
                        // 处理当前代码结构。
                        .contentType(MediaType.APPLICATION_JSON)
                        // 处理当前代码结构。
                        .content(objectMapper.writeValueAsString(Map.of(
                                // 处理当前代码结构。
                                "newPassword", "secret123",
                                // 处理当前代码结构。
                                "confirmPassword", "secret123"))))
                // 处理当前代码结构。
                .andExpect(status().isOk())
                // 执行当前语句。
                .andExpect(jsonPath("$.message").value("Password reset successful. Please use the new password next time."));
    // 结束当前代码块。
    }

    /**
     * 描述 `resetPasswordReturnsBadRequestWhenValidationFails` 操作。
     *
     * @return 无返回值。
     * @throws Exception 已声明的异常类型 `Exception`。
     */
    // 应用当前注解。
    @Test
    // 处理当前代码结构。
    void resetPasswordReturnsBadRequestWhenValidationFails() throws Exception {
        // 处理当前代码结构。
        mockMvc.perform(post("/api/v1/auth/password/reset")
                        // 处理当前代码结构。
                        .contentType(MediaType.APPLICATION_JSON)
                        // 处理当前代码结构。
                        .content(objectMapper.writeValueAsString(Map.of(
                                // 处理当前代码结构。
                                "newPassword", "",
                                // 处理当前代码结构。
                                "confirmPassword", "secret123"))))
                // 处理当前代码结构。
                .andExpect(status().isBadRequest())
                // 执行当前语句。
                .andExpect(jsonPath("$.newPassword").value("newPassword must not be blank"));
    // 结束当前代码块。
    }

    /**
     * 描述 `resetPasswordReturnsBadRequestWhenServiceRejectsRequest` 操作。
     *
     * @return 无返回值。
     * @throws Exception 已声明的异常类型 `Exception`。
     */
    // 应用当前注解。
    @Test
    // 处理当前代码结构。
    void resetPasswordReturnsBadRequestWhenServiceRejectsRequest() throws Exception {
        // 处理当前代码结构。
        doThrow(new IllegalArgumentException("The two password entries do not match."))
                // 执行当前语句。
                .when(authService).resetPassword(any(ResetPasswordRequest.class));

        // 处理当前代码结构。
        mockMvc.perform(post("/api/v1/auth/password/reset")
                        // 处理当前代码结构。
                        .contentType(MediaType.APPLICATION_JSON)
                        // 处理当前代码结构。
                        .content(objectMapper.writeValueAsString(Map.of(
                                // 处理当前代码结构。
                                "newPassword", "secret123",
                                // 处理当前代码结构。
                                "confirmPassword", "secret456"))))
                // 处理当前代码结构。
                .andExpect(status().isBadRequest())
                // 执行当前语句。
                .andExpect(jsonPath("$.error").value("The two password entries do not match."));
    // 结束当前代码块。
    }
// 结束当前代码块。
}
