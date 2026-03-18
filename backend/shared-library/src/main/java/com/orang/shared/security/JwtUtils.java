package com.orang.shared.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

public class JwtUtils {

    private final SecretKey secretKey;

    public JwtUtils(String secretKey) {
        this.secretKey = Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));
    }

    public Optional<Claims> extractClaims(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return Optional.of(claims);
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    public Optional<String> extractUserId(String token) {
        return extractClaims(token).map(Claims::getSubject);
    }

    public Optional<String> extractEmail(String token) {
        return extractClaims(token).map(claims -> claims.get("email", String.class));
    }

}
