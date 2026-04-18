// 声明当前源文件所属包。
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

// 为当前测试类启用指定扩展。
@ExtendWith(MockitoExtension.class)
// 定义 `AuthContextFilterTest` 类型。
class AuthContextFilterTest {

    // 将当前依赖替换为 Mockito 模拟对象。
    @Mock
    // 声明用户repository变量，供后续流程使用。
    private UserRepository userRepository;

    // 将当前依赖替换为 Mockito 模拟对象。
    @Mock
    // 声明filterchain变量，供后续流程使用。
    private FilterChain filterChain;

    /**
     * 处理teardown相关逻辑。
     * @throws Exception 当前流程出现异常时抛出。
     */
    // 应用 `AfterEach` 注解声明当前行为。
    @AfterEach
    void tearDown() throws Exception {
        // 调用 `clear` 完成当前步骤。
        CurrentUserHolder.clear();
    }

    /**
     * 处理dofilterinternalsets当前用户andclearsitafterchain相关逻辑。
     * @throws Exception 当前流程出现异常时抛出。
     */
    // 声明当前方法为测试用例。
    @Test
    void doFilterInternalSetsCurrentUserAndClearsItAfterChain() throws Exception {
        // 创建filter对象。
        AuthContextFilter filter = new AuthContextFilter(userRepository);
        // 创建请求对象。
        MockHttpServletRequest request = new MockHttpServletRequest();
        // 创建响应对象。
        MockHttpServletResponse response = new MockHttpServletResponse();
        // 计算并保存用户结果。
        User user = TestDataFactory.user("user-1", "tester", "token-1");
        // 调用 `addHeader` 完成当前步骤。
        request.addHeader(AuthContextFilter.AUTH_HEADER, "token-1");
        // 为当前测试场景预设模拟对象行为。
        when(userRepository.findByAuthToken("token-1")).thenReturn(Optional.of(user));
        // 调用 `doFilter` 完成当前步骤。
        filter.doFilter(request, response, filterChain);
        // 校验依赖调用是否符合预期。
        verify(filterChain).doFilter(request, response);
        // 定义当前类型。
        assertThatThrownBy(CurrentUserHolder::getRequired).isInstanceOf(UnauthorizedException.class).hasMessage("Authentication required");
    }

    /**
     * 处理dofilterinternalleaves上下文emptywhenheaderismissing相关逻辑。
     * @throws Exception 当前流程出现异常时抛出。
     */
    // 声明当前方法为测试用例。
    @Test
    void doFilterInternalLeavesContextEmptyWhenHeaderIsMissing() throws Exception {
        // 创建filter对象。
        AuthContextFilter filter = new AuthContextFilter(userRepository);
        // 创建请求对象。
        MockHttpServletRequest request = new MockHttpServletRequest();
        // 创建响应对象。
        MockHttpServletResponse response = new MockHttpServletResponse();
        // 调用 `doFilter` 完成当前步骤。
        filter.doFilter(request, response, filterChain);
        // 校验依赖调用是否符合预期。
        verify(filterChain).doFilter(request, response);
        // 定义当前类型。
        assertThatThrownBy(CurrentUserHolder::getRequired).isInstanceOf(UnauthorizedException.class).hasMessage("Authentication required");
    }
}
