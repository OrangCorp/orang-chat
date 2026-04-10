package com.orang.messageservice.config;

import com.orang.messageservice.service.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7);

        try {
            log.info("Extracting user from token: {}", token.substring(0, Math.min(50, token.length())) + "...");
            UUID userId = jwtService.extractUserId(token);
            log.info("Successfully extracted userId: {}", userId);

            UsernamePasswordAuthenticationToken authenticationToken =
                    new UsernamePasswordAuthenticationToken(
                            userId.toString(),
                            null,
                            new ArrayList<>()
                    );

            authenticationToken.setDetails(
                    new WebAuthenticationDetailsSource().buildDetails(request)
            );

            SecurityContextHolder.getContext().setAuthentication(authenticationToken);

            log.info("Authenticated user: {} for path: {} {} ", userId, request.getMethod(), request.getRequestURI());

        } catch (Exception e) {
            log.error("JWT authentication FAILED for path: {} {} - Exception: {} - Message: {}",
                    request.getMethod(), request.getRequestURI(), e.getClass().getSimpleName(), e.getMessage(), e);
            // Don't set authentication - request will be rejected as unauthenticated
        }

        filterChain.doFilter(request, response);
    }
}