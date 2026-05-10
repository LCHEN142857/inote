package com.inote.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.inote.repository.UserRepository;
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
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
@RequiredArgsConstructor
public class ReplayProtectionFilter extends OncePerRequestFilter {

    private static final long MAX_CACHED_BODY_BYTES = 1024 * 1024;

    private final ReplayProtectionService replayProtectionService;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        if (!shouldCheck(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        HttpServletRequest effectiveRequest = shouldCacheBody(request)
                ? new CachedBodyHttpServletRequest(request)
                : request;
        byte[] body = effectiveRequest instanceof CachedBodyHttpServletRequest cached
                ? cached.getCachedBody()
                : new byte[0];

        if (replayProtectionService.isReplay(fingerprint(effectiveRequest, body))) {
            reject(response);
            return;
        }

        filterChain.doFilter(effectiveRequest, response);
    }

    private boolean shouldCheck(HttpServletRequest request) {
        String path = request.getRequestURI();
        if (!path.startsWith("/api/")) {
            return false;
        }
        return isLoginRequest(request) || isMutatingRequest(request);
    }

    private boolean isLoginRequest(HttpServletRequest request) {
        return "POST".equalsIgnoreCase(request.getMethod())
                && "/api/v1/auth/login".equals(request.getRequestURI());
    }

    private boolean isMutatingRequest(HttpServletRequest request) {
        String method = request.getMethod();
        return "POST".equalsIgnoreCase(method)
                || "PUT".equalsIgnoreCase(method)
                || "PATCH".equalsIgnoreCase(method)
                || "DELETE".equalsIgnoreCase(method);
    }

    private boolean shouldCacheBody(HttpServletRequest request) {
        String contentType = request.getContentType();
        if (contentType != null && contentType.toLowerCase().startsWith(MediaType.MULTIPART_FORM_DATA_VALUE)) {
            return false;
        }

        long contentLength = request.getContentLengthLong();
        return contentLength >= 0 && contentLength <= MAX_CACHED_BODY_BYTES;
    }

    private String fingerprint(HttpServletRequest request, byte[] body) {
        if (isLoginRequest(request)) {
            return loginFingerprint(request, body);
        }
        return businessFingerprint(request, body);
    }

    private String loginFingerprint(HttpServletRequest request, byte[] body) {
        JsonNode json = readJson(body);
        String username = text(json, "username");
        String password = text(json, "password");
        String captchaCode = text(json, "captchaCode");
        return String.join("|",
                request.getRequestURI(),
                ClientIpResolver.resolve(request),
                username,
                password,
                captchaCode);
    }

    private String businessFingerprint(HttpServletRequest request, byte[] body) {
        String token = header(request, AuthContextFilter.AUTH_HEADER);
        String username = token.isBlank()
                ? ""
                : userRepository.findByAuthToken(token).map(user -> user.getUsername()).orElse("");
        return String.join("|",
                request.getRequestURI(),
                nullToEmpty(request.getQueryString()),
                ClientIpResolver.resolve(request),
                username,
                token,
                requestParameters(request, body));
    }

    private String requestParameters(HttpServletRequest request, byte[] body) {
        if (body.length > 0) {
            JsonNode json = readJson(body);
            if (!json.isMissingNode()) {
                return sortJson(json).toString();
            }
            return new String(body, StandardCharsets.UTF_8);
        }

        return String.join("|",
                nullToEmpty(request.getContentType()),
                String.valueOf(request.getContentLengthLong()));
    }

    private JsonNode readJson(byte[] body) {
        if (body.length == 0) {
            return objectMapper.missingNode();
        }
        try {
            return objectMapper.readTree(body);
        } catch (IOException e) {
            return objectMapper.missingNode();
        }
    }

    private JsonNode sortJson(JsonNode node) {
        if (node.isObject()) {
            ObjectNode sorted = objectMapper.createObjectNode();
            Map<String, JsonNode> fields = new TreeMap<>();
            Iterator<Map.Entry<String, JsonNode>> iterator = node.fields();
            while (iterator.hasNext()) {
                Map.Entry<String, JsonNode> entry = iterator.next();
                fields.put(entry.getKey(), sortJson(entry.getValue()));
            }
            fields.forEach(sorted::set);
            return sorted;
        }

        if (node.isArray()) {
            ArrayNode array = objectMapper.createArrayNode();
            node.forEach(item -> array.add(sortJson(item)));
            return array;
        }

        return node;
    }

    private String text(JsonNode json, String field) {
        if (json.isMissingNode()) {
            return "";
        }
        JsonNode value = json.get(field);
        return value == null || value.isNull() ? "" : value.asText("");
    }

    private String header(HttpServletRequest request, String name) {
        String value = request.getHeader(name);
        return value == null ? "" : value.trim();
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private void reject(HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_CONFLICT);
        response.setHeader("Retry-After", "1");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write("{\"error\":\"请求重复提交，请稍后再试。\"}");
    }
}
