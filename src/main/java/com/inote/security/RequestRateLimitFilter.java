package com.inote.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

// 在请求进入业务处理前执行基于路径和客户端 IP 的限流。
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@RequiredArgsConstructor
public class RequestRateLimitFilter extends OncePerRequestFilter {

    // 维护不同接口类型的令牌桶状态。
    private final RequestRateLimitService requestRateLimitService;

    /**
     * 拦截 HTTP 请求并在超过限流阈值时提前返回。
     * @param request 当前 HTTP 请求。
     * @param response 当前 HTTP 响应。
     * @param filterChain 后续过滤器链。
     * @throws ServletException 下游过滤器处理失败时抛出。
     * @throws IOException 响应写入或过滤器链 I/O 失败时抛出。
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        // 拒绝超过阈值的请求，避免进入控制器层消耗业务资源。
        if (!allow(request)) {
            response.setStatus(429);
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write("{\"error\":\"请求过于频繁，请稍后再试。\"}");
            return;
        }

        // 未触发限流时继续后续认证和业务处理。
        filterChain.doFilter(request, response);
    }

    /**
     * 根据请求路径和客户端 IP 判断是否允许通过。
     * @param request 当前 HTTP 请求。
     * @return true 表示允许继续处理，false 表示触发限流。
     */
    private boolean allow(HttpServletRequest request) {
        // 仅对 API 请求启用限流，避免影响静态资源或管理端点。
        String path = request.getRequestURI();
        if (!path.startsWith("/api/")) {
            return true;
        }
        // 使用解析后的客户端 IP 作为限流维度之一。
        return requestRateLimitService.allow(path, ClientIpResolver.resolve(request));
    }
}
