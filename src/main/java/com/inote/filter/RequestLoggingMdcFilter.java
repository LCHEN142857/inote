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

@Component
public class RequestLoggingMdcFilter extends OncePerRequestFilter {

    private static final String TRACE_ID_KEY = "traceId";
    private static final String ENDPOINT_ID_KEY = "endpointId";
    private static final String RUNTIME_ENDPOINT_KEY = "runtimeEndpoint";
    private static final String TRACE_ID_HEADER = "X-Trace-Id";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String traceId = resolveTraceId(request);
        String endpointId = resolveEndpointId(request.getRequestURI());

        MDC.put(TRACE_ID_KEY, traceId);
        MDC.put(ENDPOINT_ID_KEY, endpointId);
        MDC.put(RUNTIME_ENDPOINT_KEY, RuntimeEndpointHolder.getEndpoint());
        response.setHeader(TRACE_ID_HEADER, traceId);

        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(TRACE_ID_KEY);
            MDC.remove(ENDPOINT_ID_KEY);
            MDC.remove(RUNTIME_ENDPOINT_KEY);
        }
    }

    private String resolveTraceId(HttpServletRequest request) {
        String traceId = request.getHeader(TRACE_ID_HEADER);
        if (traceId == null || traceId.isBlank()) {
            return UUID.randomUUID().toString().replace("-", "");
        }
        return traceId;
    }

    private String resolveEndpointId(String requestUri) {
        if (requestUri == null || requestUri.isBlank() || "/".equals(requestUri)) {
            return "root";
        }

        String normalized = requestUri.endsWith("/") && requestUri.length() > 1
                ? requestUri.substring(0, requestUri.length() - 1)
                : requestUri;
        int lastSlashIndex = normalized.lastIndexOf('/');
        if (lastSlashIndex < 0 || lastSlashIndex == normalized.length() - 1) {
            return normalized.replace("/", "");
        }
        return normalized.substring(lastSlashIndex + 1);
    }
}
