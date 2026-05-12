package com.inote.security;

import com.inote.model.entity.User;
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

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@RequiredArgsConstructor
public class AuthContextFilter extends OncePerRequestFilter {

    public static final String AUTH_HEADER = "X-Auth-Token";

    private final UserRepository userRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String token = request.getHeader(AUTH_HEADER);

        if (!requiresAuthentication(request)) {
            loadCurrentUser(token);
            try {
                filterChain.doFilter(request, response);
            } finally {
                CurrentUserHolder.clear();
            }
            return;
        }

        if (token == null || token.isBlank()) {
            rejectUnauthorized(response);
            return;
        }

        User user = userRepository.findByAuthToken(token.trim()).orElse(null);
        if (user == null) {
            rejectUnauthorized(response);
            return;
        }

        CurrentUserHolder.set(user);
        try {
            filterChain.doFilter(request, response);
        } finally {
            CurrentUserHolder.clear();
        }
    }

    private void loadCurrentUser(String token) {
        if (token == null || token.isBlank()) {
            return;
        }
        userRepository.findByAuthToken(token.trim()).ifPresent(CurrentUserHolder::set);
    }

    private boolean requiresAuthentication(HttpServletRequest request) {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return false;
        }
        String path = request.getRequestURI();
        if (!path.startsWith("/api/v1/")) {
            return false;
        }
        return !("GET".equalsIgnoreCase(request.getMethod()) && "/api/v1/auth/captcha".equals(path))
                && !("POST".equalsIgnoreCase(request.getMethod()) && "/api/v1/auth/login".equals(path));
    }

    private void rejectUnauthorized(HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write("{\"error\":\"Authentication required\"}");
    }
}
