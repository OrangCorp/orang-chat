package com.orang.notificationservice.service;

import com.orang.shared.security.JwtUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class JwtService {

    private final JwtUtils jwtUtils;

    public JwtService(@Value("${jwt.secret}") String secret) {
        this.jwtUtils = new JwtUtils(secret);
    }

    public UUID extractUserId(String token) {
        return jwtUtils.extractUserId(token)
                .map(UUID::fromString)
                .orElseThrow(() -> new IllegalArgumentException("Invalid token or missing user ID"));
    }

    public boolean isTokenValid(String token) {
        return jwtUtils.extractClaims(token).isPresent();
    }
}
