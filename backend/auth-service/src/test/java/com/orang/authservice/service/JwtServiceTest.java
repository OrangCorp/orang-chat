package com.orang.authservice.service;

import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {

    private JwtService jwtService;

    private final long accessTokenExpiration = 900000;
    private final long refreshTokenExpiration = 604800000;
    private final String secretKey = "my-super-secret-key-for-testing-purposes-only-12345";

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(secretKey, accessTokenExpiration, refreshTokenExpiration);
    }

    @Test
    void shouldGenerateValidAccessToken() {
        UUID userId = UUID.randomUUID();
        String email = "test@example.com";

        String token = jwtService.generateToken(userId, email);

        assertNotNull(token);

        Claims claims = jwtService.validateToken(token);
        assertEquals(userId.toString(), claims.getSubject());
        assertEquals(email, claims.get("email"));
        assertEquals("access", claims.get("type"));

        long duration = claims.getExpiration().getTime() - System.currentTimeMillis();
        assertEquals(accessTokenExpiration, duration, 1000);
    }

    @Test
    void shouldGenerateValidRefreshToken() {
        UUID userId = UUID.randomUUID();
        String token = jwtService.generateRefreshToken(userId);

        assertNotNull(token);

        Claims claims = jwtService.validateToken(token);
        assertEquals(userId.toString(), claims.getSubject());
        assertEquals("refresh", claims.get("type"));
        assertNull(claims.get("email"));

        long duration = claims.getExpiration().getTime() - claims.getIssuedAt().getTime();
        assertEquals(refreshTokenExpiration, duration, 1000);
    }

    @Test
    void shouldExtractUserIdCorrectly() {
        UUID userId = UUID.randomUUID();
        String token = jwtService.generateToken(userId, "test@example.com");

        UUID extractedId = jwtService.extractUserId(token);
        assertEquals(userId, extractedId);
    }

    @Test
    void shouldIdentifyAccessTokenCorrectly() {
        UUID userId = UUID.randomUUID();
        String accessToken = jwtService.generateToken(userId, "test@example.com");

        assertTrue(jwtService.isAccessToken(accessToken));
        assertFalse(jwtService.isRefreshToken(accessToken));
    }

    @Test
    void shouldIdentifyRefreshTokenCorrectly() {
        UUID userId = UUID.randomUUID();
        String refreshToken = jwtService.generateRefreshToken(userId);

        assertTrue(jwtService.isRefreshToken(refreshToken));
        assertFalse(jwtService.isAccessToken(refreshToken));
    }

    @Test
    void shouldRejectInvalidTokens() {
        String fakeToken = "this.is.not.a.valid.jwt";

        assertFalse(jwtService.isAccessToken(fakeToken));
        assertFalse(jwtService.isRefreshToken(fakeToken));
    }

    @Test
    void shouldRejectExpiredTokens() throws InterruptedException {
        JwtService shortLivedService = new JwtService(
                "mySuperSecretKeyThatIsLongEnoughForHS256Test!",
                1,
                1000
        );

        UUID userId = UUID.randomUUID();
        String token = shortLivedService.generateToken(userId, "test@example.com");

        Thread.sleep(10);

        assertFalse(shortLivedService.isAccessToken(token));
    }

    @Test
    void shouldGenerateRefreshTokenWithJti() {
        UUID userId = UUID.randomUUID();
        String token = jwtService.generateRefreshToken(userId);

        String tokenId = jwtService.extractTokenId(token);

        assertNotNull(tokenId);
        assertDoesNotThrow(() -> UUID.fromString(tokenId));
    }

    @Test
    void shouldGenerateUniqueJtiForEachToken() {
        UUID userId = UUID.randomUUID();

        String token1 = jwtService.generateRefreshToken(userId);
        String token2 = jwtService.generateRefreshToken(userId);

        assertNotEquals(jwtService.extractTokenId(token1), jwtService.extractTokenId(token2));
    }
}
