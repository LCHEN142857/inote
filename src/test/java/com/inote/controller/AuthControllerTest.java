// 声明当前源文件所属包。
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

// 应用 `WebMvcTest` 注解声明当前行为。
@WebMvcTest(AuthController.class)
// 在测试环境中注入 MockMvc。
@AutoConfigureMockMvc(addFilters = false)
// 定义 `AuthControllerTest` 类型。
class AuthControllerTest {

    // 从 Spring 容器中注入当前依赖。
    @Autowired
    // 声明mockmvc变量，供后续流程使用。
    private MockMvc mockMvc;

    // 从 Spring 容器中注入当前依赖。
    @Autowired
    // 声明objectmapper变量，供后续流程使用。
    private ObjectMapper objectMapper;

    // 在 Spring 测试上下文中注册模拟对象。
    @MockBean
    // 声明认证service变量，供后续流程使用。
    private AuthService authService;

    // 在 Spring 测试上下文中注册模拟对象。
    @MockBean
    // 声明用户repository变量，供后续流程使用。
    private UserRepository userRepository;

    /**
     * 处理验证码returns验证码payload相关逻辑。
     * @throws Exception 当前流程出现异常时抛出。
     */
    // 声明当前方法为测试用例。
    @Test
    void captchaReturnsCaptchaPayload() throws Exception {
        // 围绕when认证service补充当前业务语句。
        when(authService.generateCaptcha()).thenReturn(AuthCaptchaResponse.builder()
                // 设置验证码id字段的取值。
                .captchaId("captcha-1")
                // 设置验证码code字段的取值。
                .captchaCode("ABCD")
                // 完成当前建造者对象的组装。
                .build());

        // 发起当前接口的集成测试请求。
        mockMvc.perform(get("/api/v1/auth/captcha"))
                // 继续校验接口响应结果。
                .andExpect(status().isOk())
                // 继续校验接口响应结果。
                .andExpect(jsonPath("$.captchaId").value("captcha-1"))
                // 继续校验接口响应结果。
                .andExpect(jsonPath("$.captchaCode").value("ABCD"));
    }

    /**
     * 处理登录returns认证响应when请求isvalid相关逻辑。
     * @throws Exception 当前流程出现异常时抛出。
     */
    // 声明当前方法为测试用例。
    @Test
    void loginReturnsAuthResponseWhenRequestIsValid() throws Exception {
        // 定义当前类型。
        when(authService.loginOrRegister(any(AuthLoginRequest.class))).thenReturn(AuthResponse.builder()
                // 设置令牌字段的取值。
                .token("token-1")
                // 设置username字段的取值。
                .username("tester")
                // 完成当前建造者对象的组装。
                .build());

        // 发起当前接口的集成测试请求。
        mockMvc.perform(post("/api/v1/auth/login")
                        // 设置内容type字段的取值。
                        .contentType(MediaType.APPLICATION_JSON)
                        // 设置内容字段的取值。
                        .content(objectMapper.writeValueAsString(Map.of(
                                "username", "tester",
                                "password", "secret123",
                                "captchaId", "captcha-1",
                                "captchaCode", "ABCD"))))
                // 继续校验接口响应结果。
                .andExpect(status().isOk())
                // 继续校验接口响应结果。
                .andExpect(jsonPath("$.token").value("token-1"))
                // 继续校验接口响应结果。
                .andExpect(jsonPath("$.username").value("tester"));
    }

    /**
     * 处理登录returnsbad请求whenvalidationfails相关逻辑。
     * @throws Exception 当前流程出现异常时抛出。
     */
    // 声明当前方法为测试用例。
    @Test
    void loginReturnsBadRequestWhenValidationFails() throws Exception {
        // 发起当前接口的集成测试请求。
        mockMvc.perform(post("/api/v1/auth/login")
                        // 设置内容type字段的取值。
                        .contentType(MediaType.APPLICATION_JSON)
                        // 设置内容字段的取值。
                        .content(objectMapper.writeValueAsString(Map.of(
                                "username", "",
                                "password", "secret123",
                                "captchaId", "captcha-1",
                                "captchaCode", "ABCD"))))
                // 继续校验接口响应结果。
                .andExpect(status().isBadRequest())
                // 继续校验接口响应结果。
                .andExpect(jsonPath("$.username").value("username must not be blank"));
    }

    /**
     * 处理登录returnsunauthorizedwhenservicerejectscredentials相关逻辑。
     * @throws Exception 当前流程出现异常时抛出。
     */
    // 声明当前方法为测试用例。
    @Test
    void loginReturnsUnauthorizedWhenServiceRejectsCredentials() throws Exception {
        // 定义当前类型。
        when(authService.loginOrRegister(any(AuthLoginRequest.class)))
                // 设置thenthrow字段的取值。
                .thenThrow(new UnauthorizedException("Username or password is incorrect."));

        // 发起当前接口的集成测试请求。
        mockMvc.perform(post("/api/v1/auth/login")
                        // 设置内容type字段的取值。
                        .contentType(MediaType.APPLICATION_JSON)
                        // 设置内容字段的取值。
                        .content(objectMapper.writeValueAsString(Map.of(
                                "username", "tester",
                                "password", "secret123",
                                "captchaId", "captcha-1",
                                "captchaCode", "ABCD"))))
                // 继续校验接口响应结果。
                .andExpect(status().isUnauthorized())
                // 继续校验接口响应结果。
                .andExpect(jsonPath("$.error").value("Username or password is incorrect."));
    }

    /**
     * 处理mereturns当前用户相关逻辑。
     * @throws Exception 当前流程出现异常时抛出。
     */
    // 声明当前方法为测试用例。
    @Test
    void meReturnsCurrentUser() throws Exception {
        // 围绕when认证service补充当前业务语句。
        when(authService.currentUser()).thenReturn(AuthResponse.builder()
                // 设置令牌字段的取值。
                .token("token-2")
                // 设置username字段的取值。
                .username("current-user")
                // 完成当前建造者对象的组装。
                .build());

        // 发起当前接口的集成测试请求。
        mockMvc.perform(get("/api/v1/auth/me"))
                // 继续校验接口响应结果。
                .andExpect(status().isOk())
                // 继续校验接口响应结果。
                .andExpect(jsonPath("$.token").value("token-2"))
                // 继续校验接口响应结果。
                .andExpect(jsonPath("$.username").value("current-user"));
    }

    /**
     * 处理mereturnsunauthorizedwhenno当前用户exists相关逻辑。
     * @throws Exception 当前流程出现异常时抛出。
     */
    // 声明当前方法为测试用例。
    @Test
    void meReturnsUnauthorizedWhenNoCurrentUserExists() throws Exception {
        // 为当前测试场景预设模拟对象行为。
        when(authService.currentUser()).thenThrow(new UnauthorizedException("Authentication required."));

        // 发起当前接口的集成测试请求。
        mockMvc.perform(get("/api/v1/auth/me"))
                // 继续校验接口响应结果。
                .andExpect(status().isUnauthorized())
                // 继续校验接口响应结果。
                .andExpect(jsonPath("$.error").value("Authentication required."));
    }

    /**
     * 处理reset密码returnssuccess消息when请求isvalid相关逻辑。
     * @throws Exception 当前流程出现异常时抛出。
     */
    // 声明当前方法为测试用例。
    @Test
    void resetPasswordReturnsSuccessMessageWhenRequestIsValid() throws Exception {
        // 定义当前类型。
        doNothing().when(authService).resetPassword(any(ResetPasswordRequest.class));

        // 发起当前接口的集成测试请求。
        mockMvc.perform(post("/api/v1/auth/password/reset")
                        // 设置内容type字段的取值。
                        .contentType(MediaType.APPLICATION_JSON)
                        // 设置内容字段的取值。
                        .content(objectMapper.writeValueAsString(Map.of(
                                "newPassword", "secret123",
                                "confirmPassword", "secret123"))))
                // 继续校验接口响应结果。
                .andExpect(status().isOk())
                // 继续校验接口响应结果。
                .andExpect(jsonPath("$.message").value("Password reset successful. Please use the new password next time."));
    }

    /**
     * 处理reset密码returnsbad请求whenvalidationfails相关逻辑。
     * @throws Exception 当前流程出现异常时抛出。
     */
    // 声明当前方法为测试用例。
    @Test
    void resetPasswordReturnsBadRequestWhenValidationFails() throws Exception {
        // 发起当前接口的集成测试请求。
        mockMvc.perform(post("/api/v1/auth/password/reset")
                        // 设置内容type字段的取值。
                        .contentType(MediaType.APPLICATION_JSON)
                        // 设置内容字段的取值。
                        .content(objectMapper.writeValueAsString(Map.of(
                                "newPassword", "",
                                "confirmPassword", "secret123"))))
                // 继续校验接口响应结果。
                .andExpect(status().isBadRequest())
                // 继续校验接口响应结果。
                .andExpect(jsonPath("$.newPassword").value("newPassword must not be blank"));
    }

    /**
     * 处理reset密码returnsbad请求whenservicerejects请求相关逻辑。
     * @throws Exception 当前流程出现异常时抛出。
     */
    // 声明当前方法为测试用例。
    @Test
    void resetPasswordReturnsBadRequestWhenServiceRejectsRequest() throws Exception {
        // 围绕dothrowillegal补充当前业务语句。
        doThrow(new IllegalArgumentException("The two password entries do not match."))
                // 定义当前类型。
                .when(authService).resetPassword(any(ResetPasswordRequest.class));

        // 发起当前接口的集成测试请求。
        mockMvc.perform(post("/api/v1/auth/password/reset")
                        // 设置内容type字段的取值。
                        .contentType(MediaType.APPLICATION_JSON)
                        // 设置内容字段的取值。
                        .content(objectMapper.writeValueAsString(Map.of(
                                "newPassword", "secret123",
                                "confirmPassword", "secret456"))))
                // 继续校验接口响应结果。
                .andExpect(status().isBadRequest())
                // 继续校验接口响应结果。
                .andExpect(jsonPath("$.error").value("The two password entries do not match."));
    }
}
