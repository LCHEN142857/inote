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

@Component
@RequiredArgsConstructor
public class AuthContextFilter extends OncePerRequestFilter {

    public static final String AUTH_HEADER = "X-Auth-Token";

    private final UserRepository userRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String token = request.getHeader(AUTH_HEADER);
        if (token != null && !token.isBlank()) {
            User user = userRepository.findByAuthToken(token.trim()).orElse(null);
            if (user != null) {
                CurrentUserHolder.set(user);
            }
        }

        try {
            filterChain.doFilter(request, response);
        } finally {
            CurrentUserHolder.clear();
        }
    }
}
