// 声明当前源文件的包。
package com.inote.filter;

import com.inote.logging.RuntimeEndpointHolder;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

// 应用当前注解。
@Component
// 声明当前类型。
public class RequestLoggingMdcFilter extends OncePerRequestFilter {

    // 声明当前字段。
    private static final String TRACE_ID_KEY = "traceId";
    // 声明当前字段。
    private static final String ENDPOINT_ID_KEY = "endpointId";
    // 声明当前字段。
    private static final String RUNTIME_ENDPOINT_KEY = "runtimeEndpoint";
    // 声明当前字段。
    private static final String TRACE_ID_HEADER = "X-Trace-Id";

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
        String traceId = resolveTraceId(request);
        // 执行当前语句。
        String endpointId = resolveEndpointId(request.getRequestURI());

        // 执行当前语句。
        MDC.put(TRACE_ID_KEY, traceId);
        // 执行当前语句。
        MDC.put(ENDPOINT_ID_KEY, endpointId);
        // 执行当前语句。
        MDC.put(RUNTIME_ENDPOINT_KEY, RuntimeEndpointHolder.getEndpoint());
        // 执行当前语句。
        response.setHeader(TRACE_ID_HEADER, traceId);

        // 执行当前流程控制分支。
        try {
            // 执行当前语句。
            filterChain.doFilter(request, response);
        // 处理当前代码结构。
        } finally {
            // 执行当前语句。
            MDC.remove(TRACE_ID_KEY);
            // 执行当前语句。
            MDC.remove(ENDPOINT_ID_KEY);
            // 执行当前语句。
            MDC.remove(RUNTIME_ENDPOINT_KEY);
        // 结束当前代码块。
        }
    // 结束当前代码块。
    }

    /**
     * 描述 `resolveTraceId` 操作。
     *
     * @param request 输入参数 `request`。
     * @return 类型为 `String` 的返回值。
     */
    // 处理当前代码结构。
    private String resolveTraceId(HttpServletRequest request) {
        // 执行当前语句。
        String traceId = request.getHeader(TRACE_ID_HEADER);
        // 执行当前流程控制分支。
        if (traceId == null || traceId.isBlank()) {
            // 返回当前结果。
            return UUID.randomUUID().toString().replace("-", "");
        // 结束当前代码块。
        }
        // 返回当前结果。
        return traceId;
    // 结束当前代码块。
    }

    /**
     * 描述 `resolveEndpointId` 操作。
     *
     * @param requestUri 输入参数 `requestUri`。
     * @return 类型为 `String` 的返回值。
     */
    // 处理当前代码结构。
    private String resolveEndpointId(String requestUri) {
        // 执行当前流程控制分支。
        if (requestUri == null || requestUri.isBlank() || "/".equals(requestUri)) {
            // 返回当前结果。
            return "root";
        // 结束当前代码块。
        }

        // 处理当前代码结构。
        String normalized = requestUri.endsWith("/") && requestUri.length() > 1
                // 处理当前代码结构。
                ? requestUri.substring(0, requestUri.length() - 1)
                // 执行当前语句。
                : requestUri;
        // 执行当前语句。
        int lastSlashIndex = normalized.lastIndexOf('/');
        // 执行当前流程控制分支。
        if (lastSlashIndex < 0 || lastSlashIndex == normalized.length() - 1) {
            // 返回当前结果。
            return normalized.replace("/", "");
        // 结束当前代码块。
        }
        // 返回当前结果。
        return normalized.substring(lastSlashIndex + 1);
    // 结束当前代码块。
    }
// 结束当前代码块。
}
