// 声明当前源文件所属包。
package com.inote.security;

import com.inote.model.entity.User;
import com.inote.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

// 将当前类注册为通用组件。
@Component
// 让 Lombok 为当前类生成必填依赖构造函数。
@RequiredArgsConstructor
// 定义认证过滤器，负责根据令牌加载当前用户上下文。
public class AuthContextFilter extends OncePerRequestFilter {

    // 计算并保存认证header结果。
    public static final String AUTH_HEADER = "X-Auth-Token";

    // 声明用户repository变量，供后续流程使用。
    private final UserRepository userRepository;

    /**
     * 处理dofilterinternal相关逻辑。
     * @param request 请求参数。
     * @param response 响应参数。
     * @param filterChain filterchain参数。
     * @throws ServletException Servlet 过滤链执行失败时抛出。
     * @throws IOException 文件读写失败时抛出。
     */
    // 声明当前方法重写父类或接口定义。
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        // 计算并保存令牌结果。
        String token = request.getHeader(AUTH_HEADER);
        // 根据条件判断当前分支是否执行。
        if (token != null && !token.isBlank()) {
            // 查询用户数据。
            User user = userRepository.findByAuthToken(token.trim()).orElse(null);
            // 根据条件判断当前分支是否执行。
            if (user != null) {
                // 调用 `set` 完成当前步骤。
                CurrentUserHolder.set(user);
            }
        }

        // 进入异常保护块执行关键逻辑。
        try {
            // 调用 `doFilter` 完成当前步骤。
            filterChain.doFilter(request, response);
        } finally {
            // 调用 `clear` 完成当前步骤。
            CurrentUserHolder.clear();
        }
    }
}
