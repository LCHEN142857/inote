package com.inote.security;

import jakarta.servlet.http.HttpServletRequest;

// 从请求头或远端地址中提取客户端 IP。
public final class ClientIpResolver {

    // 禁止实例化工具类。
    private ClientIpResolver() {
    }

    /**
     * 提取最可信的客户端 IP。
     * @param request 当前 HTTP 请求。
     * @return 客户端 IP 或代理转发链中的首个地址。
     */
    public static String resolve(HttpServletRequest request) {
        // 优先使用代理链中的第一个真实来源地址。
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            // 代理列表中第一个值通常代表原始客户端。
            String[] parts = forwardedFor.split(",");
            if (parts.length > 0 && !parts[0].isBlank()) {
                return parts[0].trim();
            }
        }

        // 次优先使用网关注入的单一真实 IP。
        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return realIp.trim();
        }

        // 最后退回到容器感知到的远端地址。
        return request.getRemoteAddr();
    }
}
