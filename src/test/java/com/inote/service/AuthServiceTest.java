// 声明当前源文件的包。
package com.inote.service;

import com.inote.config.AuthProperties;
import com.inote.model.dto.AuthCaptchaResponse;
import com.inote.model.dto.AuthLoginRequest;
import com.inote.model.dto.ResetPasswordRequest;
import com.inote.model.entity.User;
import com.inote.repository.UserRepository;
import com.inote.security.CurrentUserService;
import com.inote.security.UnauthorizedException;
import com.inote.support.TestDataFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// 应用当前注解。
@ExtendWith(MockitoExtension.class)
// 声明当前类型。
class AuthServiceTest {

    // 应用当前注解。
    @Mock
    // 声明当前字段。
    private UserRepository userRepository;

    // 应用当前注解。
    @Mock
    // 声明当前字段。
    private PasswordEncoder passwordEncoder;

    // 应用当前注解。
    @Mock
    // 声明当前字段。
    private CurrentUserService currentUserService;

    // 应用当前注解。
    @InjectMocks
    // 声明当前字段。
    private AuthService authService;

    // 声明当前字段。
    private AuthProperties authProperties;

    /**
     * 描述 `setUp` 操作。
     *
     * @return 无返回值。
     * @throws Exception 已声明的异常类型 `Exception`。
     */
    // 应用当前注解。
    @BeforeEach
    // 处理当前代码结构。
    void setUp() throws Exception {
        // 执行当前语句。
        authProperties = new AuthProperties();
        // 执行当前语句。
        authProperties.setCaptchaLength(6);
        // 执行当前语句。
        org.springframework.test.util.ReflectionTestUtils.setField(authService, "authProperties", authProperties);
    // 结束当前代码块。
    }

    /**
     * 描述 `generateCaptchaUsesMinimumLengthConstraint` 操作。
     *
     * @return 无返回值。
     * @throws Exception 已声明的异常类型 `Exception`。
     */
    // 应用当前注解。
    @Test
    // 处理当前代码结构。
    void generateCaptchaUsesMinimumLengthConstraint() throws Exception {
        // 执行当前语句。
        authProperties.setCaptchaLength(2);
        // 执行当前语句。
        AuthCaptchaResponse response = authService.generateCaptcha();
        // 执行当前语句。
        assertThat(response.getCaptchaId()).isNotBlank();
        // 执行当前语句。
        assertThat(response.getCaptchaCode()).hasSize(4);
    // 结束当前代码块。
    }

    /**
     * 描述 `loginOrRegisterRegistersNewUserWhenUsernameDoesNotExist` 操作。
     *
     * @return 无返回值。
     * @throws Exception 已声明的异常类型 `Exception`。
     */
    // 应用当前注解。
    @Test
    // 处理当前代码结构。
    void loginOrRegisterRegistersNewUserWhenUsernameDoesNotExist() throws Exception {
        // 执行当前语句。
        AuthCaptchaResponse captcha = authService.generateCaptcha();
        // 执行当前语句。
        when(userRepository.findByUsername("tester")).thenReturn(Optional.empty());
        // 执行当前语句。
        when(passwordEncoder.encode("secret123")).thenReturn("encoded-secret");
        // 执行当前语句。
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0, User.class));
        // 执行当前语句。
        AuthLoginRequest request = AuthLoginRequest.builder().username("Tester").password("secret123").captchaId(captcha.getCaptchaId()).captchaCode(captcha.getCaptchaCode()).build();
        // 执行当前语句。
        var response = authService.loginOrRegister(request);
        // 执行当前语句。
        ArgumentCaptor<User> savedUserCaptor = ArgumentCaptor.forClass(User.class);
        // 执行当前语句。
        verify(userRepository).save(savedUserCaptor.capture());
        // 执行当前语句。
        assertThat(response.getUsername()).isEqualTo("tester");
        // 执行当前语句。
        assertThat(response.getToken()).isNotBlank();
        // 执行当前语句。
        assertThat(savedUserCaptor.getValue().getPasswordHash()).isEqualTo("encoded-secret");
        // 执行当前语句。
        assertThat(savedUserCaptor.getValue().getAuthToken()).isEqualTo(response.getToken());
    // 结束当前代码块。
    }

    /**
     * 描述 `loginOrRegisterThrowsUnauthorizedWhenPasswordDoesNotMatch` 操作。
     *
     * @return 无返回值。
     * @throws Exception 已声明的异常类型 `Exception`。
     */
    // 应用当前注解。
    @Test
    // 处理当前代码结构。
    void loginOrRegisterThrowsUnauthorizedWhenPasswordDoesNotMatch() throws Exception {
        // 执行当前语句。
        AuthCaptchaResponse captcha = authService.generateCaptcha();
        // 执行当前语句。
        User existingUser = TestDataFactory.user("user-1", "tester", "token-1");
        // 执行当前语句。
        existingUser.setPasswordHash("encoded-secret");
        // 执行当前语句。
        when(userRepository.findByUsername("tester")).thenReturn(Optional.of(existingUser));
        // 执行当前语句。
        when(passwordEncoder.matches("wrong-pass", "encoded-secret")).thenReturn(false);
        // 执行当前语句。
        AuthLoginRequest request = AuthLoginRequest.builder().username("tester").password("wrong-pass").captchaId(captcha.getCaptchaId()).captchaCode(captcha.getCaptchaCode()).build();
        // 执行当前语句。
        assertThatThrownBy(() -> authService.loginOrRegister(request)).isInstanceOf(UnauthorizedException.class).hasMessage("Username or password is incorrect.");
        // 执行当前语句。
        verify(userRepository, never()).save(any(User.class));
    // 结束当前代码块。
    }

    /**
     * 描述 `resetPasswordRejectsMismatchedConfirmation` 操作。
     *
     * @return 无返回值。
     * @throws Exception 已声明的异常类型 `Exception`。
     */
    // 应用当前注解。
    @Test
    // 处理当前代码结构。
    void resetPasswordRejectsMismatchedConfirmation() throws Exception {
        // 执行当前语句。
        ResetPasswordRequest request = ResetPasswordRequest.builder().newPassword("secret123").confirmPassword("secret456").build();
        // 执行当前语句。
        assertThatThrownBy(() -> authService.resetPassword(request)).isInstanceOf(IllegalArgumentException.class).hasMessage("The two password entries do not match.");
    // 结束当前代码块。
    }

    /**
     * 描述 `resetPasswordUpdatesPasswordHashAndToken` 操作。
     *
     * @return 无返回值。
     * @throws Exception 已声明的异常类型 `Exception`。
     */
    // 应用当前注解。
    @Test
    // 处理当前代码结构。
    void resetPasswordUpdatesPasswordHashAndToken() throws Exception {
        // 执行当前语句。
        User currentUser = TestDataFactory.user("user-1", "tester", "old-token");
        // 执行当前语句。
        when(currentUserService.getCurrentUser()).thenReturn(currentUser);
        // 执行当前语句。
        when(passwordEncoder.encode("secret123")).thenReturn("encoded-secret");
        // 执行当前语句。
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0, User.class));
        // 执行当前语句。
        ResetPasswordRequest request = ResetPasswordRequest.builder().newPassword("secret123").confirmPassword("secret123").build();
        // 执行当前语句。
        authService.resetPassword(request);
        // 执行当前语句。
        assertThat(currentUser.getPasswordHash()).isEqualTo("encoded-secret");
        // 执行当前语句。
        assertThat(currentUser.getAuthToken()).isNotEqualTo("old-token");
        // 执行当前语句。
        verify(userRepository).save(currentUser);
    // 结束当前代码块。
    }
// 结束当前代码块。
}
