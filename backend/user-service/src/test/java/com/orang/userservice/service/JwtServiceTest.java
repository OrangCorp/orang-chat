package com.orang.userservice.service;

import io.jsonwebtoken.Jwts;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {

    private static final String SECRET = "01234567890123456789012345678901";

    @Test
    @DisplayName("validateToken and extractUserId work for signed tokens")
    void validateTokenAndExtractUserIdWorkForSignedTokens() {
        JwtService jwtService = new JwtService(SECRET);
        SecretKey secretKey = io.jsonwebtoken.security.Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
        UUID userId = UUID.fromString("844ec9f6-f781-4f67-aab0-1f33cf9734f7");

        String token = Jwts.builder()
                .subject(userId.toString())
                .signWith(secretKey)
                .compact();

        assertThat(jwtService.isTokenValid(token)).isTrue();
        assertThat(jwtService.extractUserId(token)).isEqualTo(userId);
        assertThat(jwtService.validateToken(token).getSubject()).isEqualTo(userId.toString());
    }

    @Test
    @DisplayName("invalid token is rejected")
    void invalidTokenIsRejected() {
        JwtService jwtService = new JwtService(SECRET);

        assertThat(jwtService.isTokenValid("not-a-token")).isFalse();
    }
}