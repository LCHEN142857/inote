// 声明当前源文件的包。
package com.inote.security;

import com.inote.model.entity.User;
import com.inote.repository.UserRepository;
import com.inote.support.TestDataFactory;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// 应用当前注解。
@ExtendWith(MockitoExtension.class)
// 声明当前类型。
class AuthContextFilterTest {

    // 应用当前注解。
    @Mock
    // 声明当前字段。
    private UserRepository userRepository;

    // 应用当前注解。
    @Mock
    // 声明当前字段。
    private FilterChain filterChain;

    /**
     * 描述 `tearDown` 操作。
     *
     * @return 无返回值。
     * @throws Exception 已声明的异常类型 `Exception`。
     */
    // 应用当前注解。
    @AfterEach
    // 处理当前代码结构。
    void tearDown() throws Exception {
        // 执行当前语句。
        CurrentUserHolder.clear();
    // 结束当前代码块。
    }

    /**
     * 描述 `doFilterInternalSetsCurrentUserAndClearsItAfterChain` 操作。
     *
     * @return 无返回值。
     * @throws Exception 已声明的异常类型 `Exception`。
     */
    // 应用当前注解。
    @Test
    // 处理当前代码结构。
    void doFilterInternalSetsCurrentUserAndClearsItAfterChain() throws Exception {
        // 执行当前语句。
        AuthContextFilter filter = new AuthContextFilter(userRepository);
        // 执行当前语句。
        MockHttpServletRequest request = new MockHttpServletRequest();
        // 执行当前语句。
        MockHttpServletResponse response = new MockHttpServletResponse();
        // 执行当前语句。
        User user = TestDataFactory.user("user-1", "tester", "token-1");
        // 执行当前语句。
        request.addHeader(AuthContextFilter.AUTH_HEADER, "token-1");
        // 执行当前语句。
        when(userRepository.findByAuthToken("token-1")).thenReturn(Optional.of(user));
        // 执行当前语句。
        filter.doFilter(request, response, filterChain);
        // 执行当前语句。
        verify(filterChain).doFilter(request, response);
        // 执行当前语句。
        assertThatThrownBy(CurrentUserHolder::getRequired).isInstanceOf(UnauthorizedException.class).hasMessage("Authentication required");
    // 结束当前代码块。
    }

    /**
     * 描述 `doFilterInternalLeavesContextEmptyWhenHeaderIsMissing` 操作。
     *
     * @return 无返回值。
     * @throws Exception 已声明的异常类型 `Exception`。
     */
    // 应用当前注解。
    @Test
    // 处理当前代码结构。
    void doFilterInternalLeavesContextEmptyWhenHeaderIsMissing() throws Exception {
        // 执行当前语句。
        AuthContextFilter filter = new AuthContextFilter(userRepository);
        // 执行当前语句。
        MockHttpServletRequest request = new MockHttpServletRequest();
        // 执行当前语句。
        MockHttpServletResponse response = new MockHttpServletResponse();
        // 执行当前语句。
        filter.doFilter(request, response, filterChain);
        // 执行当前语句。
        verify(filterChain).doFilter(request, response);
        // 执行当前语句。
        assertThatThrownBy(CurrentUserHolder::getRequired).isInstanceOf(UnauthorizedException.class).hasMessage("Authentication required");
    // 结束当前代码块。
    }
// 结束当前代码块。
}
