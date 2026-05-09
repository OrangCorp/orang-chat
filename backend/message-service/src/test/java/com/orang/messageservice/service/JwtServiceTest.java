package com.orang.messageservice.service;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {

    private JwtService jwtService;
    private String secret;
    private UUID testUserId;

    @BeforeEach
    void setUp() {
        secret = "my-secret-key-that-is-at-least-32-characters-long-for-HS256";
        jwtService = new JwtService(secret);
        testUserId = UUID.randomUUID();
    }

    // ============ validateToken Tests ============

    @Test
    void validateTokenSuccessfullyValidatesValidToken() {
        SecretKey key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));

        String token = Jwts.builder()
                .subject(testUserId.toString())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 3600000))
                .signWith(key)
                .compact();

        var claims = jwtService.validateToken(token);

        assertNotNull(claims);
        assertEquals(testUserId.toString(), claims.getSubject());
    }

    @Test
    void validateTokenThrowsForInvalidToken() {
        String invalidToken = "invalid.token.here";

        assertThrows(Exception.class, () -> jwtService.validateToken(invalidToken));
    }

    @Test
    void validateTokenThrowsForExpiredToken() {
        SecretKey key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));

        String expiredToken = Jwts.builder()
                .subject(testUserId.toString())
                .issuedAt(new Date(System.currentTimeMillis() - 7200000))
                .expiration(new Date(System.currentTimeMillis() - 3600000)) // Expired 1 hour ago
                .signWith(key)
                .compact();

        assertThrows(Exception.class, () -> jwtService.validateToken(expiredToken));
    }

    // ============ extractUserId Tests ============

    @Test
    void extractUserIdExtractsCorrectUI() {
        SecretKey key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));

        String token = Jwts.builder()
                .subject(testUserId.toString())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 3600000))
                .signWith(key)
                .compact();

        UUID extractedUserId = jwtService.extractUserId(token);

        assertEquals(testUserId, extractedUserId);
    }

    @Test
    void extractUserIdThrowsForInvalidUUID() {
        SecretKey key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));

        String tokenWithInvalidUUID = Jwts.builder()
                .subject("not-a-uuid")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 3600000))
                .signWith(key)
                .compact();

        assertThrows(IllegalArgumentException.class, () -> jwtService.extractUserId(tokenWithInvalidUUID));
    }

    // ============ isTokenValid Tests ============

    @Test
    void isTokenValidReturnsTrueForValidToken() {
        SecretKey key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));

        String token = Jwts.builder()
                .subject(testUserId.toString())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 3600000))
                .signWith(key)
                .compact();

        assertTrue(jwtService.isTokenValid(token));
    }

    @Test
    void isTokenValidReturnsFalseForInvalidToken() {
        assertFalse(jwtService.isTokenValid("invalid.token.here"));
    }

    @Test
    void isTokenValidReturnsFalseForExpiredToken() {
        SecretKey key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));

        String expiredToken = Jwts.builder()
                .subject(testUserId.toString())
                .issuedAt(new Date(System.currentTimeMillis() - 7200000))
                .expiration(new Date(System.currentTimeMillis() - 3600000))
                .signWith(key)
                .compact();

        assertFalse(jwtService.isTokenValid(expiredToken));
    }

    @Test
    void isTokenValidReturnsFalseForNullToken() {
        assertFalse(jwtService.isTokenValid(null));
    }

    @Test
    void isTokenValidReturnsFalseForEmptyToken() {
        assertFalse(jwtService.isTokenValid(""));
    }
}
