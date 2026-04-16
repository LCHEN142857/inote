// 声明测试类所在包。
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

// 标记当前类使用 Mockito 扩展。
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    // 声明用户仓储模拟对象。
    @Mock
    private UserRepository userRepository;

    // 声明密码编码器模拟对象。
    @Mock
    private PasswordEncoder passwordEncoder;

    // 声明当前用户服务模拟对象。
    @Mock
    private CurrentUserService currentUserService;

    // 声明被测服务实例。
    @InjectMocks
    private AuthService authService;

    // 声明认证属性配置对象。
    private AuthProperties authProperties;

    /**
     * 初始化认证测试环境。
     * @param none 无入参。
     * @return void 无返回值。
     * @throws Exception 当反射写入字段失败时抛出异常。
     */
    @BeforeEach
    void setUp() throws Exception {
        // 创建认证属性配置对象。
        authProperties = new AuthProperties();
        // 设置验证码长度配置。
        authProperties.setCaptchaLength(6);
        // 通过反射写入被测服务的配置字段。
        org.springframework.test.util.ReflectionTestUtils.setField(authService, "authProperties", authProperties);
    }

    /**
     * 验证生成验证码时会返回最少四位字符。
     * @param none 无入参。
     * @return void 无返回值。
     * @throws Exception 当前用例不抛出受检异常。
     */
    @Test
    void generateCaptchaUsesMinimumLengthConstraint() throws Exception {
        // 设置较小的验证码长度以覆盖最小值逻辑。
        authProperties.setCaptchaLength(2);
        // 调用生成验证码方法。
        AuthCaptchaResponse response = authService.generateCaptcha();
        // 断言验证码编号已生成。
        assertThat(response.getCaptchaId()).isNotBlank();
        // 断言验证码长度至少为四位。
        assertThat(response.getCaptchaCode()).hasSize(4);
    }

    /**
     * 验证合法验证码会触发注册流程并返回新令牌。
     * @param none 无入参。
     * @return void 无返回值。
     * @throws Exception 当前用例不抛出受检异常。
     */
    @Test
    void loginOrRegisterRegistersNewUserWhenUsernameDoesNotExist() throws Exception {
        // 生成可用验证码。
        AuthCaptchaResponse captcha = authService.generateCaptcha();
        // 模拟用户名查询结果为空。
        when(userRepository.findByUsername("tester")).thenReturn(Optional.empty());
        // 模拟密码编码结果。
        when(passwordEncoder.encode("secret123")).thenReturn("encoded-secret");
        // 模拟仓储保存后返回带令牌的实体。
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0, User.class));
        // 构造登录请求对象。
        AuthLoginRequest request = AuthLoginRequest.builder().username("Tester").password("secret123").captchaId(captcha.getCaptchaId()).captchaCode(captcha.getCaptchaCode()).build();
        // 调用登录或注册方法。
        var response = authService.loginOrRegister(request);
        // 捕获被保存的用户实体。
        ArgumentCaptor<User> savedUserCaptor = ArgumentCaptor.forClass(User.class);
        // 断言仓储保存被调用。
        verify(userRepository).save(savedUserCaptor.capture());
        // 断言返回用户名已规范化。
        assertThat(response.getUsername()).isEqualTo("tester");
        // 断言返回令牌已生成。
        assertThat(response.getToken()).isNotBlank();
        // 断言保存的密码哈希来自编码器。
        assertThat(savedUserCaptor.getValue().getPasswordHash()).isEqualTo("encoded-secret");
        // 断言保存的令牌已写入用户实体。
        assertThat(savedUserCaptor.getValue().getAuthToken()).isEqualTo(response.getToken());
    }

    /**
     * 验证错误密码会抛出未授权异常。
     * @param none 无入参。
     * @return void 无返回值。
     * @throws Exception 当前用例不抛出受检异常。
     */
    @Test
    void loginOrRegisterThrowsUnauthorizedWhenPasswordDoesNotMatch() throws Exception {
        // 生成可用验证码。
        AuthCaptchaResponse captcha = authService.generateCaptcha();
        // 构造已存在用户。
        User existingUser = TestDataFactory.user("user-1", "tester", "token-1");
        // 设置已存在用户密码哈希。
        existingUser.setPasswordHash("encoded-secret");
        // 模拟用户名查询返回已存在用户。
        when(userRepository.findByUsername("tester")).thenReturn(Optional.of(existingUser));
        // 模拟密码比对失败。
        when(passwordEncoder.matches("wrong-pass", "encoded-secret")).thenReturn(false);
        // 构造错误密码请求。
        AuthLoginRequest request = AuthLoginRequest.builder().username("tester").password("wrong-pass").captchaId(captcha.getCaptchaId()).captchaCode(captcha.getCaptchaCode()).build();
        // 断言调用会抛出未授权异常。
        assertThatThrownBy(() -> authService.loginOrRegister(request)).isInstanceOf(UnauthorizedException.class).hasMessage("Username or password is incorrect.");
        // 断言失败时不会保存用户。
        verify(userRepository, never()).save(any(User.class));
    }

    /**
     * 验证重置密码时会校验两次输入是否一致。
     * @param none 无入参。
     * @return void 无返回值。
     * @throws Exception 当前用例不抛出受检异常。
     */
    @Test
    void resetPasswordRejectsMismatchedConfirmation() throws Exception {
        // 构造不一致的密码重置请求。
        ResetPasswordRequest request = ResetPasswordRequest.builder().newPassword("secret123").confirmPassword("secret456").build();
        // 断言调用会抛出参数异常。
        assertThatThrownBy(() -> authService.resetPassword(request)).isInstanceOf(IllegalArgumentException.class).hasMessage("The two password entries do not match.");
    }

    /**
     * 验证重置密码成功时会刷新密码哈希和认证令牌。
     * @param none 无入参。
     * @return void 无返回值。
     * @throws Exception 当前用例不抛出受检异常。
     */
    @Test
    void resetPasswordUpdatesPasswordHashAndToken() throws Exception {
        // 构造当前登录用户。
        User currentUser = TestDataFactory.user("user-1", "tester", "old-token");
        // 模拟当前用户服务返回当前用户。
        when(currentUserService.getCurrentUser()).thenReturn(currentUser);
        // 模拟密码编码结果。
        when(passwordEncoder.encode("secret123")).thenReturn("encoded-secret");
        // 模拟保存操作返回同一实体。
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0, User.class));
        // 构造合法密码重置请求。
        ResetPasswordRequest request = ResetPasswordRequest.builder().newPassword("secret123").confirmPassword("secret123").build();
        // 调用重置密码方法。
        authService.resetPassword(request);
        // 断言新密码哈希已写入用户。
        assertThat(currentUser.getPasswordHash()).isEqualTo("encoded-secret");
        // 断言令牌已被刷新。
        assertThat(currentUser.getAuthToken()).isNotEqualTo("old-token");
        // 断言用户变更已持久化。
        verify(userRepository).save(currentUser);
    }
}
