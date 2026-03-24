package com.orang.shared.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class JwtUtilsTest {

    private static final String TEST_SECRET = "my-super-secret-key-for-testing-purposes-only-12345";

    private JwtUtils jwtUtils;

    @BeforeEach
    void setUp() {
        jwtUtils = new JwtUtils(TEST_SECRET);
    }

    @Test
    @DisplayName("extractUserId returns user ID from valid token")
    void extractUserId_WithValidToken_ReturnsUserId() {
        UUID expectedUserId = UUID.randomUUID();
        String token = createValidToken(expectedUserId, "test@example.com");

        Optional<String> result = jwtUtils.extractUserId(token);

        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(expectedUserId.toString());
    }

    @Test
    @DisplayName("extractUserId returns empty for invalid token")
    void extractUserId_WithInvalidToken_ReturnsEmpty() {
        String invalidToken = "not.a.valid.jwt.token";

        Optional<String> result = jwtUtils.extractUserId(invalidToken);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("extractUserId returns empty for expired token")
    void extractUserId_WithExpiredToken_ReturnsEmpty() {
        UUID expectedUserId = UUID.randomUUID();
        String expiredToken = createExpiredToken(expectedUserId, "test@example.com");

        Optional<String> result = jwtUtils.extractUserId(expiredToken);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("extractUserId returns empty for token with wrong signature")
    void extractUserId_WithWrongSignature_ReturnsEmpty() {
        String wrongSecret = "completely-different-secret-key-that-is-long-enough";
        SecretKey wrongKey = Keys.hmacShaKeyFor(wrongSecret.getBytes(StandardCharsets.UTF_8));

        String tokenWithWrongSignature = Jwts.builder()
                .subject(UUID.randomUUID().toString())
                .claim("email", "hacker@evil.com")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 3600000))
                .signWith(wrongKey)
                .compact();

        Optional<String> result = jwtUtils.extractUserId(tokenWithWrongSignature);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("extractEmail returns email from valid token")
    void extractEmail_WithValidToken_ReturnsEmail() {
        UUID userId = UUID.randomUUID();
        String expectedEmail = "user@example.com";
        String token = createValidToken(userId, expectedEmail);

        Optional<String> result = jwtUtils.extractEmail(token);

        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(expectedEmail);
    }

    private String createExpiredToken(UUID expectedUserId, String mail) {
        SecretKey key = Keys.hmacShaKeyFor(TEST_SECRET.getBytes(StandardCharsets.UTF_8));
        Date pastDate = new Date(System.currentTimeMillis() - 3600000);
        Date expiredDate = new Date(System.currentTimeMillis() - 1800000);

        return Jwts.builder()
                .subject(expectedUserId.toString())
                .claim("email", mail)
                .issuedAt(pastDate)
                .expiration(expiredDate)
                .signWith(key)
                .compact();
    }

    private String createValidToken(UUID userId, String email) {
        SecretKey key = Keys.hmacShaKeyFor(TEST_SECRET.getBytes(StandardCharsets.UTF_8));
        Date now = new Date();
        Date expiration = new Date(now.getTime() + 3600000);

        return Jwts.builder()
                .subject(userId.toString())
                .claim("email", email)
                .issuedAt(now)
                .expiration(expiration)
                .signWith(key)
                .compact();
    }
}