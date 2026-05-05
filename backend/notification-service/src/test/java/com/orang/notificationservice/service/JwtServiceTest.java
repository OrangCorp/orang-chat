package com.orang.notificationservice.service;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    private static final String SECRET = "notification-service-jwt-secret-key-which-is-long-enough";

    private final JwtService jwtService = new JwtService(SECRET);

    @Test
    @DisplayName("extractUserId and isTokenValid work for signed tokens")
    void extractUserIdAndIsTokenValidWorkForSignedTokens() {
        UUID userId = UUID.fromString("844ec9f6-f781-4f67-aab0-1f33cf9734f7");
        String token = Jwts.builder()
                .subject(userId.toString())
                .issuedAt(new Date())
                .signWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8)))
                .compact();

        assertThat(jwtService.isTokenValid(token)).isTrue();
        assertThat(jwtService.extractUserId(token)).isEqualTo(userId);
    }

    @Test
    @DisplayName("invalid tokens are rejected")
    void invalidTokensAreRejected() {
        assertThat(jwtService.isTokenValid("not-a-token")).isFalse();
        assertThatThrownBy(() -> jwtService.extractUserId("not-a-token"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid token");
    }
}