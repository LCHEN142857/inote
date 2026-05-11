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

// 通过请求体和身份信息生成指纹，拦截短时间内的重复提交。
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
@RequiredArgsConstructor
public class ReplayProtectionFilter extends OncePerRequestFilter {

    // 超过该大小的 body 不参与缓存，避免占用过多内存。
    private static final long MAX_CACHED_BODY_BYTES = 1024 * 1024;

    // 提供重复提交判定能力。
    private final ReplayProtectionService replayProtectionService;
    // 用于在业务指纹中还原当前用户身份。
    private final UserRepository userRepository;
    // 负责解析和排序 JSON 请求体。
    private final ObjectMapper objectMapper;

    /**
     * 在请求进入业务层前进行重复提交检查。
     * @param request 当前 HTTP 请求。
     * @param response 当前 HTTP 响应。
     * @param filterChain 后续过滤器链。
     * @throws ServletException 下游过滤器处理失败时抛出。
     * @throws IOException 缓存请求体或过滤器链 I/O 失败时抛出。
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        // 仅对需要保护的 API 请求做重复提交判定。
        if (!shouldCheck(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        // 对可缓存的小请求体进行包装，以便后续多次读取。
        HttpServletRequest effectiveRequest = shouldCacheBody(request)
                ? new CachedBodyHttpServletRequest(request)
                : request;
        // 需要指纹计算时优先使用缓存 body，否则退化为空 body。
        byte[] body = effectiveRequest instanceof CachedBodyHttpServletRequest cached
                ? cached.getCachedBody()
                : new byte[0];

        // 命中重放窗口时直接拒绝。
        if (replayProtectionService.isReplay(fingerprint(effectiveRequest, body))) {
            reject(response);
            return;
        }

        // 未命中时继续执行业务链路。
        filterChain.doFilter(effectiveRequest, response);
    }

    /**
     * 判断当前请求是否需要做重复提交检查。
     * @param request 当前 HTTP 请求。
     * @return true 表示需要检查，false 表示跳过。
     */
    private boolean shouldCheck(HttpServletRequest request) {
        // 只保护 API 请求，避免影响其他资源。
        String path = request.getRequestURI();
        if (!path.startsWith("/api/")) {
            return false;
        }
        // 登录和修改类请求最需要重复提交保护。
        return isLoginRequest(request) || isMutatingRequest(request);
    }

    /**
     * 判断是否为登录接口请求。
     * @param request 当前 HTTP 请求。
     * @return true 表示登录请求。
     */
    private boolean isLoginRequest(HttpServletRequest request) {
        return "POST".equalsIgnoreCase(request.getMethod())
                && "/api/v1/auth/login".equals(request.getRequestURI());
    }

    /**
     * 判断是否为会修改状态的请求。
     * @param request 当前 HTTP 请求。
     * @return true 表示需要保护的写操作请求。
     */
    private boolean isMutatingRequest(HttpServletRequest request) {
        // 写操作接口更容易受到重复提交影响。
        String method = request.getMethod();
        return "POST".equalsIgnoreCase(method)
                || "PUT".equalsIgnoreCase(method)
                || "PATCH".equalsIgnoreCase(method)
                || "DELETE".equalsIgnoreCase(method);
    }

    /**
     * 判断请求体是否适合缓存。
     * @param request 当前 HTTP 请求。
     * @return true 表示可以缓存 body。
     */
    private boolean shouldCacheBody(HttpServletRequest request) {
        // Multipart 请求通常不适合在这里缓存，避免破坏上传流。
        String contentType = request.getContentType();
        if (contentType != null && contentType.toLowerCase().startsWith(MediaType.MULTIPART_FORM_DATA_VALUE)) {
            return false;
        }

        // 仅缓存大小可控的 body，避免一次性读入过大内容。
        long contentLength = request.getContentLengthLong();
        return contentLength >= 0 && contentLength <= MAX_CACHED_BODY_BYTES;
    }

    /**
     * 生成请求指纹。
     * @param request 当前 HTTP 请求。
     * @param body 请求体字节。
     * @return 用于重放判定的指纹字符串。
     */
    private String fingerprint(HttpServletRequest request, byte[] body) {
        // 登录请求需要把账号和验证码纳入指纹，避免同样请求被重复接受。
        if (isLoginRequest(request)) {
            return loginFingerprint(request, body);
        }
        // 其他业务请求使用通用业务指纹。
        return businessFingerprint(request, body);
    }

    /**
     * 为登录请求生成专用指纹。
     * @param request 当前 HTTP 请求。
     * @param body 请求体字节。
     * @return 登录请求指纹。
     */
    private String loginFingerprint(HttpServletRequest request, byte[] body) {
        // 登录请求的重复提交风险主要来自账号、密码和验证码。
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

    /**
     * 为普通业务请求生成指纹。
     * @param request 当前 HTTP 请求。
     * @param body 请求体字节。
     * @return 业务请求指纹。
     */
    private String businessFingerprint(HttpServletRequest request, byte[] body) {
        // 使用认证令牌恢复用户名，使重复提交判断更贴近真实用户维度。
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

    /**
     * 序列化请求参数以参与指纹计算。
     * @param request 当前 HTTP 请求。
     * @param body 请求体字节。
     * @return 标准化后的参数表示。
     */
    private String requestParameters(HttpServletRequest request, byte[] body) {
        // body 存在时优先解析 JSON，以便对字段顺序做归一化。
        if (body.length > 0) {
            JsonNode json = readJson(body);
            if (!json.isMissingNode()) {
                return sortJson(json).toString();
            }
            // 非 JSON body 直接使用原始文本。
            return new String(body, StandardCharsets.UTF_8);
        }

        // 无 body 时使用内容类型和长度维持指纹稳定性。
        return String.join("|",
                nullToEmpty(request.getContentType()),
                String.valueOf(request.getContentLengthLong()));
    }

    /**
     * 将请求体解析为 JSON。
     * @param body 请求体字节。
     * @return 解析后的 JSON 节点或 missingNode。
     */
    private JsonNode readJson(byte[] body) {
        if (body.length == 0) {
            return objectMapper.missingNode();
        }
        try {
            // 解析失败时回退到 missingNode，避免中断过滤链。
            return objectMapper.readTree(body);
        } catch (IOException e) {
            return objectMapper.missingNode();
        }
    }

    /**
     * 对 JSON 对象进行递归排序。
     * @param node 原始 JSON 节点。
     * @return 排序后的 JSON 节点。
     */
    private JsonNode sortJson(JsonNode node) {
        // 对象字段按字典序排序，降低字段顺序对指纹的影响。
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

        // 数组保持元素顺序，但对子节点继续递归归一化。
        if (node.isArray()) {
            ArrayNode array = objectMapper.createArrayNode();
            node.forEach(item -> array.add(sortJson(item)));
            return array;
        }

        return node;
    }

    /**
     * 读取 JSON 字段文本值。
     * @param json JSON 节点。
     * @param field 字段名。
     * @return 字段文本值，缺失时返回空串。
     */
    private String text(JsonNode json, String field) {
        if (json.isMissingNode()) {
            return "";
        }
        JsonNode value = json.get(field);
        return value == null || value.isNull() ? "" : value.asText("");
    }

    /**
     * 安全读取请求头。
     * @param request 当前 HTTP 请求。
     * @param name 头名称。
     * @return 头值，缺失时返回空串。
     */
    private String header(HttpServletRequest request, String name) {
        String value = request.getHeader(name);
        return value == null ? "" : value.trim();
    }

    /**
     * 将空值转换为空字符串。
     * @param value 原始字符串。
     * @return 非空字符串。
     */
    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    /**
     * 返回重复提交拒绝响应。
     * @param response 当前 HTTP 响应。
     * @throws IOException 写响应失败时抛出。
     */
    private void reject(HttpServletResponse response) throws IOException {
        // 409 更适合表达幂等冲突而不是服务端故障。
        response.setStatus(HttpServletResponse.SC_CONFLICT);
        // 告知客户端稍后再试。
        response.setHeader("Retry-After", "1");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write("{\"error\":\"请求重复提交，请稍后再试。\"}");
    }
}
