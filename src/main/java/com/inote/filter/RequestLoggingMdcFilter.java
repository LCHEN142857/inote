// 声明当前源文件所属包。
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

// 将当前类注册为通用组件。
@Component
// 定义请求日志过滤器，负责向 MDC 写入请求上下文。
public class RequestLoggingMdcFilter extends OncePerRequestFilter {

    // 计算并保存traceidkey结果。
    private static final String TRACE_ID_KEY = "traceId";
    // 计算并保存端点idkey结果。
    private static final String ENDPOINT_ID_KEY = "endpointId";
    // 计算并保存运行时端点key结果。
    private static final String RUNTIME_ENDPOINT_KEY = "runtimeEndpoint";
    // 计算并保存traceidheader结果。
    private static final String TRACE_ID_HEADER = "X-Trace-Id";

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
        // 计算并保存traceid结果。
        String traceId = resolveTraceId(request);
        // 计算并保存端点id结果。
        String endpointId = resolveEndpointId(request.getRequestURI());

        // 写入当前映射中的键值对。
        MDC.put(TRACE_ID_KEY, traceId);
        // 写入当前映射中的键值对。
        MDC.put(ENDPOINT_ID_KEY, endpointId);
        // 写入当前映射中的键值对。
        MDC.put(RUNTIME_ENDPOINT_KEY, RuntimeEndpointHolder.getEndpoint());
        // 更新header字段。
        response.setHeader(TRACE_ID_HEADER, traceId);

        // 进入异常保护块执行关键逻辑。
        try {
            // 调用 `doFilter` 完成当前步骤。
            filterChain.doFilter(request, response);
        } finally {
            // 调用 `remove` 完成当前步骤。
            MDC.remove(TRACE_ID_KEY);
            // 调用 `remove` 完成当前步骤。
            MDC.remove(ENDPOINT_ID_KEY);
            // 调用 `remove` 完成当前步骤。
            MDC.remove(RUNTIME_ENDPOINT_KEY);
        }
    }

    /**
     * 处理resolvetraceid相关逻辑。
     * @param request 请求参数。
     * @return 处理后的字符串结果。
     */
    private String resolveTraceId(HttpServletRequest request) {
        // 计算并保存traceid结果。
        String traceId = request.getHeader(TRACE_ID_HEADER);
        // 根据条件判断当前分支是否执行。
        if (traceId == null || traceId.isBlank()) {
            // 返回 `randomUUID` 的处理结果。
            return UUID.randomUUID().toString().replace("-", "");
        }
        // 返回traceid。
        return traceId;
    }

    /**
     * 处理resolve端点id相关逻辑。
     * @param requestUri 请求uri参数。
     * @return 处理后的字符串结果。
     */
    private String resolveEndpointId(String requestUri) {
        // 根据条件判断当前分支是否执行。
        if (requestUri == null || requestUri.isBlank() || "/".equals(requestUri)) {
            // 返回"root"。
            return "root";
        }

        // 围绕normalized请求uri补充当前业务语句。
        String normalized = requestUri.endsWith("/") && requestUri.length() > 1
                ? requestUri.substring(0, requestUri.length() - 1)
                : requestUri;
        // 计算并保存lastslashindex结果。
        int lastSlashIndex = normalized.lastIndexOf('/');
        // 根据条件判断当前分支是否执行。
        if (lastSlashIndex < 0 || lastSlashIndex == normalized.length() - 1) {
            // 返回 `replace` 的处理结果。
            return normalized.replace("/", "");
        }
        // 返回 `substring` 的处理结果。
        return normalized.substring(lastSlashIndex + 1);
    }
}
