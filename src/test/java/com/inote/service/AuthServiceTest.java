package com.inote.service;

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

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private CurrentUserService currentUserService;

    @Mock
    private CaptchaImageRenderer captchaImageRenderer;

    @Mock
    private LoginLockService loginLockService;

    @InjectMocks
    private AuthService authService;

    @BeforeEach
    void setUp() {
        when(captchaImageRenderer.renderAsDataUri(any(String.class))).thenReturn("data:image/png;base64,ZmFrZS1pbWFnZQ==");
    }

    @Test
    void generateCaptchaProducesFourCharacterCodeAndImage() throws Exception {
        AuthCaptchaResponse response = authService.generateCaptcha();

        assertThat(response.getCaptchaId()).isNotBlank();
        assertThat(response.getCaptchaImage()).startsWith("data:image/png;base64,");
        assertThat(readCaptchaCode(response.getCaptchaId())).hasSize(4);
    }

    @Test
    void loginOrRegisterRegistersNewUserWhenUsernameDoesNotExist() throws Exception {
        AuthCaptchaResponse captcha = authService.generateCaptcha();
        String captchaCode = readCaptchaCode(captcha.getCaptchaId());
        when(userRepository.findByUsername("tester")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("secret123")).thenReturn("encoded-secret");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0, User.class));

        AuthLoginRequest request = AuthLoginRequest.builder()
                .username("Tester")
                .password("secret123")
                .captchaId(captcha.getCaptchaId())
                .captchaCode(captchaCode)
                .build();

        var response = authService.loginOrRegister(request, "127.0.0.1");

        ArgumentCaptor<User> savedUserCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(savedUserCaptor.capture());
        assertThat(response.getUsername()).isEqualTo("tester");
        assertThat(response.getToken()).isNotBlank();
        assertThat(savedUserCaptor.getValue().getPasswordHash()).isEqualTo("encoded-secret");
        assertThat(savedUserCaptor.getValue().getAuthToken()).isEqualTo(response.getToken());
    }

    @Test
    void loginOrRegisterThrowsUnauthorizedWhenPasswordDoesNotMatch() throws Exception {
        AuthCaptchaResponse captcha = authService.generateCaptcha();
        String captchaCode = readCaptchaCode(captcha.getCaptchaId());
        User existingUser = TestDataFactory.user("user-1", "tester", "token-1");
        existingUser.setPasswordHash("encoded-secret");
        when(userRepository.findByUsername("tester")).thenReturn(Optional.of(existingUser));
        when(passwordEncoder.matches("wrong-pass", "encoded-secret")).thenReturn(false);

        AuthLoginRequest request = AuthLoginRequest.builder()
                .username("tester")
                .password("wrong-pass")
                .captchaId(captcha.getCaptchaId())
                .captchaCode(captchaCode)
                .build();

        assertThatThrownBy(() -> authService.loginOrRegister(request, "127.0.0.1"))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("Username or password is incorrect.");
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void resetPasswordRejectsMismatchedConfirmation() {
        ResetPasswordRequest request = ResetPasswordRequest.builder()
                .newPassword("secret123")
                .confirmPassword("secret456")
                .build();

        assertThatThrownBy(() -> authService.resetPassword(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("The two password entries do not match.");
    }

    @Test
    void resetPasswordUpdatesPasswordHashAndToken() {
        User currentUser = TestDataFactory.user("user-1", "tester", "old-token");
        when(currentUserService.getCurrentUser()).thenReturn(currentUser);
        when(passwordEncoder.encode("secret123")).thenReturn("encoded-secret");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0, User.class));

        ResetPasswordRequest request = ResetPasswordRequest.builder()
                .newPassword("secret123")
                .confirmPassword("secret123")
                .build();

        authService.resetPassword(request);

        assertThat(currentUser.getPasswordHash()).isEqualTo("encoded-secret");
        assertThat(currentUser.getAuthToken()).isNotEqualTo("old-token");
        verify(userRepository).save(currentUser);
    }

    private String readCaptchaCode(String captchaId) throws Exception {
        Field captchasField = AuthService.class.getDeclaredField("captchas");
        captchasField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<String, Object> captchas = (Map<String, Object>) captchasField.get(authService);
        Object entry = captchas.get(captchaId);
        Method codeMethod = entry.getClass().getDeclaredMethod("code");
        codeMethod.setAccessible(true);
        return (String) codeMethod.invoke(entry);
    }
}
