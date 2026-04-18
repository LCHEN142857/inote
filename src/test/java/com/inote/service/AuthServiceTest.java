// 声明当前源文件所属包。
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

// 为当前测试类启用指定扩展。
@ExtendWith(MockitoExtension.class)
// 定义 `AuthServiceTest` 类型。
class AuthServiceTest {

    // 将当前依赖替换为 Mockito 模拟对象。
    @Mock
    // 声明用户repository变量，供后续流程使用。
    private UserRepository userRepository;

    // 将当前依赖替换为 Mockito 模拟对象。
    @Mock
    // 声明密码encoder变量，供后续流程使用。
    private PasswordEncoder passwordEncoder;

    // 将当前依赖替换为 Mockito 模拟对象。
    @Mock
    // 声明当前用户service变量，供后续流程使用。
    private CurrentUserService currentUserService;

    // 将模拟依赖注入被测对象。
    @InjectMocks
    // 声明认证service变量，供后续流程使用。
    private AuthService authService;

    // 声明认证properties变量，供后续流程使用。
    private AuthProperties authProperties;

    /**
     * 处理setup相关逻辑。
     * @throws Exception 当前流程出现异常时抛出。
     */
    // 声明当前方法在每个测试前执行。
    @BeforeEach
    void setUp() throws Exception {
        // 创建认证properties对象。
        authProperties = new AuthProperties();
        // 更新验证码length字段。
        authProperties.setCaptchaLength(6);
        // 更新field字段。
        org.springframework.test.util.ReflectionTestUtils.setField(authService, "authProperties", authProperties);
    }

    /**
     * 处理generate验证码usesminimumlengthconstraint相关逻辑。
     * @throws Exception 当前流程出现异常时抛出。
     */
    // 声明当前方法为测试用例。
    @Test
    void generateCaptchaUsesMinimumLengthConstraint() throws Exception {
        // 更新验证码length字段。
        authProperties.setCaptchaLength(2);
        // 计算并保存响应结果。
        AuthCaptchaResponse response = authService.generateCaptcha();
        // 断言当前结果符合测试预期。
        assertThat(response.getCaptchaId()).isNotBlank();
        // 断言当前结果符合测试预期。
        assertThat(response.getCaptchaCode()).hasSize(4);
    }

    /**
     * 处理登录or注册registersnew用户whenusernamedoesnotexist相关逻辑。
     * @throws Exception 当前流程出现异常时抛出。
     */
    // 声明当前方法为测试用例。
    @Test
    void loginOrRegisterRegistersNewUserWhenUsernameDoesNotExist() throws Exception {
        // 计算并保存验证码结果。
        AuthCaptchaResponse captcha = authService.generateCaptcha();
        // 为当前测试场景预设模拟对象行为。
        when(userRepository.findByUsername("tester")).thenReturn(Optional.empty());
        // 为当前测试场景预设模拟对象行为。
        when(passwordEncoder.encode("secret123")).thenReturn("encoded-secret");
        // 定义当前类型。
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0, User.class));
        // 开始构建请求对象。
        AuthLoginRequest request = AuthLoginRequest.builder().username("Tester").password("secret123").captchaId(captcha.getCaptchaId()).captchaCode(captcha.getCaptchaCode()).build();
        // 计算并保存响应结果。
        var response = authService.loginOrRegister(request);
        // 定义当前类型。
        ArgumentCaptor<User> savedUserCaptor = ArgumentCaptor.forClass(User.class);
        // 校验依赖调用是否符合预期。
        verify(userRepository).save(savedUserCaptor.capture());
        // 断言当前结果符合测试预期。
        assertThat(response.getUsername()).isEqualTo("tester");
        // 断言当前结果符合测试预期。
        assertThat(response.getToken()).isNotBlank();
        // 断言当前结果符合测试预期。
        assertThat(savedUserCaptor.getValue().getPasswordHash()).isEqualTo("encoded-secret");
        // 断言当前结果符合测试预期。
        assertThat(savedUserCaptor.getValue().getAuthToken()).isEqualTo(response.getToken());
    }

    /**
     * 处理登录or注册throwsunauthorizedwhen密码doesnotmatch相关逻辑。
     * @throws Exception 当前流程出现异常时抛出。
     */
    // 声明当前方法为测试用例。
    @Test
    void loginOrRegisterThrowsUnauthorizedWhenPasswordDoesNotMatch() throws Exception {
        // 计算并保存验证码结果。
        AuthCaptchaResponse captcha = authService.generateCaptcha();
        // 计算并保存existing用户结果。
        User existingUser = TestDataFactory.user("user-1", "tester", "token-1");
        // 更新密码hash字段。
        existingUser.setPasswordHash("encoded-secret");
        // 为当前测试场景预设模拟对象行为。
        when(userRepository.findByUsername("tester")).thenReturn(Optional.of(existingUser));
        // 为当前测试场景预设模拟对象行为。
        when(passwordEncoder.matches("wrong-pass", "encoded-secret")).thenReturn(false);
        // 开始构建请求对象。
        AuthLoginRequest request = AuthLoginRequest.builder().username("tester").password("wrong-pass").captchaId(captcha.getCaptchaId()).captchaCode(captcha.getCaptchaCode()).build();
        // 定义当前类型。
        assertThatThrownBy(() -> authService.loginOrRegister(request)).isInstanceOf(UnauthorizedException.class).hasMessage("Username or password is incorrect.");
        // 定义当前类型。
        verify(userRepository, never()).save(any(User.class));
    }

    /**
     * 处理reset密码rejectsmismatchedconfirmation相关逻辑。
     * @throws Exception 当前流程出现异常时抛出。
     */
    // 声明当前方法为测试用例。
    @Test
    void resetPasswordRejectsMismatchedConfirmation() throws Exception {
        // 开始构建请求对象。
        ResetPasswordRequest request = ResetPasswordRequest.builder().newPassword("secret123").confirmPassword("secret456").build();
        // 定义当前类型。
        assertThatThrownBy(() -> authService.resetPassword(request)).isInstanceOf(IllegalArgumentException.class).hasMessage("The two password entries do not match.");
    }

    /**
     * 处理reset密码updates密码hashand令牌相关逻辑。
     * @throws Exception 当前流程出现异常时抛出。
     */
    // 声明当前方法为测试用例。
    @Test
    void resetPasswordUpdatesPasswordHashAndToken() throws Exception {
        // 计算并保存当前用户结果。
        User currentUser = TestDataFactory.user("user-1", "tester", "old-token");
        // 为当前测试场景预设模拟对象行为。
        when(currentUserService.getCurrentUser()).thenReturn(currentUser);
        // 为当前测试场景预设模拟对象行为。
        when(passwordEncoder.encode("secret123")).thenReturn("encoded-secret");
        // 定义当前类型。
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0, User.class));
        // 开始构建请求对象。
        ResetPasswordRequest request = ResetPasswordRequest.builder().newPassword("secret123").confirmPassword("secret123").build();
        // 调用 `resetPassword` 完成当前步骤。
        authService.resetPassword(request);
        // 断言当前结果符合测试预期。
        assertThat(currentUser.getPasswordHash()).isEqualTo("encoded-secret");
        // 断言当前结果符合测试预期。
        assertThat(currentUser.getAuthToken()).isNotEqualTo("old-token");
        // 校验依赖调用是否符合预期。
        verify(userRepository).save(currentUser);
    }
}
