package com.orang.authservice.service;

import com.orang.shared.exception.BadRequestException;
import com.orang.shared.exception.TooManyRequestsException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailTokenService {

    private final RedisTemplate<String, String> redisTemplate;
    private final Environment environment;
    private final SecureRandom secureRandom = new SecureRandom();

    private static final String DEV_CODE = "672137";

    @Value("${app.verification.code-expiry-minutes}")
    private long codeExpiryMinutes;

    @Value("${app.verification.resend-cooldown-seconds}")
    private long resendCooldownSeconds;

    @Value("${app.verification.max-attempts}")
    private int maxAttempts;

    @Value("${app.password-reset.token-expiry-hours}")
    private long resetTokenExpiryHours;

    private boolean isDevProfile() {
        return Arrays.asList(environment.getActiveProfiles()).contains("dev");
    }

    // ==================== Key Builders ====================

    private String verificationCodeKey(String email) {
        return "email:verification:" + email.toLowerCase() + ":code";
    }

    private String verificationAttemptsKey(String email) {
        return "email:verification:" + email.toLowerCase() + ":attempts";
    }

    private String verificationCooldownKey(String email) {
        return "email:verification:" + email.toLowerCase() + ":cooldown";
    }

    private String resetTokenKey(String token) {
        return "email:reset:" + token;
    }

    // ==================== Verification Code ====================

    public String generateVerificationCode(String email) {
        if (isOnCooldown(email)) {
            throw new TooManyRequestsException("Please wait before requesting a new code");
        }

        String codeStr;
        if (isDevProfile()) {
            codeStr = DEV_CODE;
            log.info("DEV MODE: Verification code for {} is {}", email, codeStr);
        } else {
            int code = 100000 + secureRandom.nextInt(900000);
            codeStr = String.valueOf(code);
        }

        redisTemplate.opsForValue().set(
                verificationCodeKey(email),
                codeStr,
                codeExpiryMinutes,
                TimeUnit.MINUTES
        );

        redisTemplate.delete(verificationAttemptsKey(email));

        redisTemplate.opsForValue().set(
                verificationCooldownKey(email),
                "1",
                resendCooldownSeconds,
                TimeUnit.SECONDS
        );

        log.debug("Verification code generated for {}", email);
        return codeStr;
    }

    public boolean verifyCode(String email, String submittedCode) {
        String attemptsKey = verificationAttemptsKey(email);

        String attemptsStr = redisTemplate.opsForValue().get(attemptsKey);
        int attempts = attemptsStr != null ? Integer.parseInt(attemptsStr) : 0;

        if (attempts >= maxAttempts) {
            throw new TooManyRequestsException("Too many attempts. Please request a new code.");
        }

        redisTemplate.opsForValue().increment(attemptsKey);
        redisTemplate.expire(attemptsKey, codeExpiryMinutes, TimeUnit.MINUTES);

        String storedCode = redisTemplate.opsForValue().get(verificationCodeKey(email));

        if (storedCode == null) {
            throw new BadRequestException("Verification code expired or not found. Please request a new one.");
        }

        boolean matches = MessageDigest.isEqual(
                storedCode.getBytes(StandardCharsets.UTF_8),
                submittedCode.getBytes(StandardCharsets.UTF_8)
        );

        if (matches) {
            redisTemplate.delete(verificationCodeKey(email));
            redisTemplate.delete(verificationAttemptsKey(email));
            log.info("Email verification successful for {}", email);
        }

        return matches;
    }

    public boolean isOnCooldown(String email) {
        return Boolean.TRUE.equals(
                redisTemplate.hasKey(verificationCooldownKey(email))
        );
    }

    // ==================== Password Reset Token ====================

    public String generateResetToken(String email) {
        byte[] tokenBytes = new byte[32];
        secureRandom.nextBytes(tokenBytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);

        redisTemplate.opsForValue().set(
                resetTokenKey(token),
                email.toLowerCase(),
                resetTokenExpiryHours,
                TimeUnit.HOURS
        );

        log.debug("Password reset token generated for {}", email);
        return token;
    }

    public String validateResetToken(String token) {
        String email = redisTemplate.opsForValue().get(resetTokenKey(token));

        if (email == null) {
            throw new BadRequestException("Reset token is invalid or expired");
        }

        return email;
    }

    public void invalidateResetToken(String token) {
        redisTemplate.delete(resetTokenKey(token));
    }
}