package com.benwyw.bot.config.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Set;

@Component
public class AuthFilter extends OncePerRequestFilter {
    private static final Logger logger = LoggerFactory.getLogger(AuthFilter.class);

    // Define paths that don't require authentication
    public static final Set<String> PUBLIC_PATHS = Set.of(
            "/auth/login",
            "/auth/refresh",
            "/auth/logout",
            "/health",
            "/actuator/**",
            "/swagger-ui/**",
            "/v3/api-docs/**",
            "/misc/userBase",
            "/whity/getLatestWhityWeight"
    );

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getServletPath();

        // Skip authentication for public paths
        if (shouldSkipAuthentication(path)) {
            filterChain.doFilter(request, response);
            return;
        }

        // Get JWT from Authorization header
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            String username = JwtUtil.validateAccessToken(token);

            if (username != null) {
                // Valid token, create authentication object
                List<SimpleGrantedAuthority> authorities =
                        Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"));

                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(username, null, authorities);

                SecurityContextHolder.getContext().setAuthentication(authentication);
                logger.debug("Set authentication for user: {}", username);
            } else {
                logger.debug("Invalid access token");
            }
        } else {
            logger.debug("No Authorization header found or not a Bearer token");
        }

        filterChain.doFilter(request, response);
    }

    private boolean shouldSkipAuthentication(String path) {
        return PUBLIC_PATHS.stream()
                .anyMatch(publicPath -> {
                    if (publicPath.endsWith("/**")) {
                        String basePath = publicPath.substring(0, publicPath.length() - 3);
                        return path.startsWith(basePath);
                    }
                    return path.equals(publicPath);
                });
    }
}
