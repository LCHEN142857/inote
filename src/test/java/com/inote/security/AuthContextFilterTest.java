// 声明测试类所在包。
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

// 标记当前类使用 Mockito 扩展。
@ExtendWith(MockitoExtension.class)
class AuthContextFilterTest {

    // 声明用户仓储模拟对象。
    @Mock
    private UserRepository userRepository;

    // 声明过滤器链模拟对象。
    @Mock
    private FilterChain filterChain;

    /**
     * 每个用例执行后清理线程上下文。
     * @param none 无入参。
     * @return void 无返回值。
     * @throws Exception 当前用例不抛出受检异常。
     */
    @AfterEach
    void tearDown() throws Exception {
        // 清理当前用户上下文。
        CurrentUserHolder.clear();
    }

    /**
     * 验证存在合法令牌时过滤器会设置并最终清理当前用户。
     * @param none 无入参。
     * @return void 无返回值。
     * @throws Exception 当过滤器执行链抛出异常时向外抛出。
     */
    @Test
    void doFilterInternalSetsCurrentUserAndClearsItAfterChain() throws Exception {
        // 构造过滤器实例。
        AuthContextFilter filter = new AuthContextFilter(userRepository);
        // 构造请求对象。
        MockHttpServletRequest request = new MockHttpServletRequest();
        // 构造响应对象。
        MockHttpServletResponse response = new MockHttpServletResponse();
        // 构造测试用户实体。
        User user = TestDataFactory.user("user-1", "tester", "token-1");
        // 在请求头中设置令牌。
        request.addHeader(AuthContextFilter.AUTH_HEADER, "token-1");
        // 模拟按令牌查询到用户。
        when(userRepository.findByAuthToken("token-1")).thenReturn(Optional.of(user));
        // 调用过滤器。
        filter.doFilter(request, response, filterChain);
        // 断言过滤链被执行。
        verify(filterChain).doFilter(request, response);
        // 断言过滤完成后线程上下文已被清理。
        assertThatThrownBy(CurrentUserHolder::getRequired).isInstanceOf(UnauthorizedException.class).hasMessage("Authentication required");
    }

    /**
     * 验证没有令牌时过滤器只透传请求并保持上下文为空。
     * @param none 无入参。
     * @return void 无返回值。
     * @throws Exception 当过滤器执行链抛出异常时向外抛出。
     */
    @Test
    void doFilterInternalLeavesContextEmptyWhenHeaderIsMissing() throws Exception {
        // 构造过滤器实例。
        AuthContextFilter filter = new AuthContextFilter(userRepository);
        // 构造请求对象。
        MockHttpServletRequest request = new MockHttpServletRequest();
        // 构造响应对象。
        MockHttpServletResponse response = new MockHttpServletResponse();
        // 调用过滤器。
        filter.doFilter(request, response, filterChain);
        // 断言过滤链被执行。
        verify(filterChain).doFilter(request, response);
        // 断言没有用户进入上下文。
        assertThatThrownBy(CurrentUserHolder::getRequired).isInstanceOf(UnauthorizedException.class).hasMessage("Authentication required");
    }
}
