package com.sharecycle.config;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.sharecycle.domain.model.User;
import com.sharecycle.domain.repository.UserRepository;
import com.sharecycle.service.SessionStore;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Minimal session-token based authentication filter that looks for the Authorization header,
 * validates it against the SessionStore, and populates the Spring Security context.
 */
@Component
public class SessionAuthenticationFilter extends OncePerRequestFilter {

    private final SessionStore sessionStore;
    private final UserRepository userRepository;

    public SessionAuthenticationFilter(SessionStore sessionStore, UserRepository userRepository) {
        this.sessionStore = sessionStore;
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        if (isPublicPath(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = resolveToken(request);
        if (token != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            UUID userId = sessionStore.getUserId(token);
            if (userId != null) {
                User user = userRepository.findById(userId);
                if (user != null) {
                    // Get effective role considering operator mode
                    String effectiveRole = sessionStore.getEffectiveRole(token, user.getRole());
                    var authorities = List.of(new SimpleGrantedAuthority("ROLE_" + effectiveRole));
                    var authentication = new UsernamePasswordAuthenticationToken(user, token, authorities);
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            }
        }

        filterChain.doFilter(request, response);
    }

    private boolean isPublicPath(HttpServletRequest request) {
        String method = request.getMethod();
        if ("OPTIONS".equalsIgnoreCase(method)) {
            return true;
        }
        String path = request.getRequestURI();
        return path.startsWith("/api/auth")
                || path.startsWith("/api/public")
                || "/health".equals(path);
    }

    private String resolveToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header != null && !header.isBlank()) {
            String value = header.trim();
            if (value.startsWith("Bearer ")) {
                value = value.substring(7);
            }
            if (!value.isBlank()) {
                return value;
            }
        }

        String param = request.getParameter("token");
        if (param != null && !param.isBlank()) {
            return param.trim();
        }

        return null;
    }
}
