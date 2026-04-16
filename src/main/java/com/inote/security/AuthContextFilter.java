// 声明当前源文件的包。
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

// 应用当前注解。
@Component
// 应用当前注解。
@RequiredArgsConstructor
// 声明当前类型。
public class AuthContextFilter extends OncePerRequestFilter {

    // 声明当前字段。
    public static final String AUTH_HEADER = "X-Auth-Token";

    // 声明当前字段。
    private final UserRepository userRepository;

    /**
     * 描述 `doFilterInternal` 操作。
     *
     * @param request 输入参数 `request`。
     * @param response 输入参数 `response`。
     * @param filterChain 输入参数 `filterChain`。
     * @return 无返回值。
     * @throws ServletException 已声明的异常类型 `ServletException`。
     * @throws IOException 已声明的异常类型 `IOException`。
     */
    // 应用当前注解。
    @Override
    // 处理当前代码结构。
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            // 处理当前代码结构。
            throws ServletException, IOException {
        // 执行当前语句。
        String token = request.getHeader(AUTH_HEADER);
        // 执行当前流程控制分支。
        if (token != null && !token.isBlank()) {
            // 执行当前语句。
            User user = userRepository.findByAuthToken(token.trim()).orElse(null);
            // 执行当前流程控制分支。
            if (user != null) {
                // 执行当前语句。
                CurrentUserHolder.set(user);
            // 结束当前代码块。
            }
        // 结束当前代码块。
        }

        // 执行当前流程控制分支。
        try {
            // 执行当前语句。
            filterChain.doFilter(request, response);
        // 处理当前代码结构。
        } finally {
            // 执行当前语句。
            CurrentUserHolder.clear();
        // 结束当前代码块。
        }
    // 结束当前代码块。
    }
// 结束当前代码块。
}
