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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// 验证认证控制器在不同输入和异常条件下的 HTTP 行为。
@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthService authService;

    @MockBean
    private UserRepository userRepository;

    // 验证验证码接口返回完整的验证码载荷。
    @Test
    void captchaReturnsCaptchaPayload() throws Exception {
        when(authService.generateCaptcha()).thenReturn(AuthCaptchaResponse.builder()
                .captchaId("captcha-1")
                .captchaImage("data:image/png;base64,ZmFrZS1pbWFnZQ==")
                .build());

        mockMvc.perform(get("/api/v1/auth/captcha"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.captchaId").value("captcha-1"))
                .andExpect(jsonPath("$.captchaImage").value("data:image/png;base64,ZmFrZS1pbWFnZQ=="));
    }

    // 验证登录接口在请求合法时返回认证结果。
    @Test
    void loginReturnsAuthResponseWhenRequestIsValid() throws Exception {
        when(authService.loginOrRegister(any(AuthLoginRequest.class), anyString())).thenReturn(AuthResponse.builder()
                .token("token-1")
                .username("tester")
                .build());

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "username", "tester",
                                "password", "secret123",
                                "captchaId", "captcha-1",
                                "captchaCode", "ABCD"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("token-1"))
                .andExpect(jsonPath("$.username").value("tester"));
    }

    // 验证登录接口会把表单校验错误返回给前端。
    @Test
    void loginReturnsBadRequestWhenValidationFails() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "username", "",
                                "password", "secret123",
                                "captchaId", "captcha-1",
                                "captchaCode", "ABCD"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.username").value("username must not be blank"));
    }

    // 验证认证服务拒绝凭据时接口返回 401。
    @Test
    void loginReturnsUnauthorizedWhenServiceRejectsCredentials() throws Exception {
        when(authService.loginOrRegister(any(AuthLoginRequest.class), anyString()))
                .thenThrow(new UnauthorizedException("Username or password is incorrect."));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "username", "tester",
                                "password", "secret123",
                                "captchaId", "captcha-1",
                                "captchaCode", "ABCD"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Username or password is incorrect."));
    }

    // 验证当前用户接口返回登录态信息。
    @Test
    void meReturnsCurrentUser() throws Exception {
        when(authService.currentUser()).thenReturn(AuthResponse.builder()
                .token("token-2")
                .username("current-user")
                .build());

        mockMvc.perform(get("/api/v1/auth/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("token-2"))
                .andExpect(jsonPath("$.username").value("current-user"));
    }

    // 验证没有登录态时当前用户接口返回 401。
    @Test
    void meReturnsUnauthorizedWhenNoCurrentUserExists() throws Exception {
        when(authService.currentUser()).thenThrow(new UnauthorizedException("Authentication required."));

        mockMvc.perform(get("/api/v1/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Authentication required."));
    }

    // 验证重置密码接口在请求合法时返回成功消息。
    @Test
    void resetPasswordReturnsSuccessMessageWhenRequestIsValid() throws Exception {
        doNothing().when(authService).resetPassword(any(ResetPasswordRequest.class));

        mockMvc.perform(post("/api/v1/auth/password/reset")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "newPassword", "secret123",
                                "confirmPassword", "secret123"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Password reset successful. Please use the new password next time."));
    }

    // 验证重置密码接口会返回表单校验错误。
    @Test
    void resetPasswordReturnsBadRequestWhenValidationFails() throws Exception {
        mockMvc.perform(post("/api/v1/auth/password/reset")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "newPassword", "",
                                "confirmPassword", "secret123"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.newPassword").value("newPassword must not be blank"));
    }

    // 验证服务层拒绝重置请求时返回业务错误。
    @Test
    void resetPasswordReturnsBadRequestWhenServiceRejectsRequest() throws Exception {
        doThrow(new IllegalArgumentException("The two password entries do not match."))
                .when(authService).resetPassword(any(ResetPasswordRequest.class));

        mockMvc.perform(post("/api/v1/auth/password/reset")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "newPassword", "secret123",
                                "confirmPassword", "secret456"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("The two password entries do not match."));
    }
}
