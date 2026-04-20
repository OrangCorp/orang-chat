package com.orang.authservice.service;

import com.orang.shared.exception.BadRequestException;
import com.orang.shared.exception.TooManyRequestsException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.core.env.Environment;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class EmailTokenServiceTest {

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private Environment environment;

    @InjectMocks
    private EmailTokenService emailTokenService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(emailTokenService, "codeExpiryMinutes", 15L);
        ReflectionTestUtils.setField(emailTokenService, "resendCooldownSeconds", 60L);
        ReflectionTestUtils.setField(emailTokenService, "maxAttempts", 5);
        ReflectionTestUtils.setField(emailTokenService, "resetTokenExpiryHours", 1L);

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(environment.getActiveProfiles()).thenReturn(new String[]{"test"});
    }

    @Test
    @DisplayName("generateVerificationCode throws on cooldown")
    void generateVerificationCode_OnCooldown_ThrowsTooManyRequests() {
        when(redisTemplate.hasKey("email:verification:user@example.com:cooldown")).thenReturn(true);

        assertThatThrownBy(() -> emailTokenService.generateVerificationCode("user@example.com"))
                .isInstanceOf(TooManyRequestsException.class)
                .hasMessage("Please wait before requesting a new code");
    }

    @Test
    @DisplayName("generateVerificationCode stores code and cooldown")
    void generateVerificationCode_StoresCodeAndCooldown() {
        when(redisTemplate.hasKey("email:verification:user@example.com:cooldown")).thenReturn(false);

        String code = emailTokenService.generateVerificationCode("user@example.com");

        assertThat(code).matches("\\d{6}");
        verify(valueOperations).set(eq("email:verification:user@example.com:code"), eq(code), eq(15L), eq(TimeUnit.MINUTES));
        verify(redisTemplate).delete("email:verification:user@example.com:attempts");
        verify(valueOperations).set("email:verification:user@example.com:cooldown", "1", 60L, TimeUnit.SECONDS);
    }

    @Test
    @DisplayName("verifyCode returns true and clears keys when code matches")
    void verifyCode_WithMatchingCode_ReturnsTrue() {
        when(valueOperations.get("email:verification:user@example.com:attempts")).thenReturn("1");
        when(valueOperations.get("email:verification:user@example.com:code")).thenReturn("123456");

        boolean result = emailTokenService.verifyCode("user@example.com", "123456");

        assertThat(result).isTrue();
        verify(valueOperations).increment("email:verification:user@example.com:attempts");
        verify(redisTemplate).expire("email:verification:user@example.com:attempts", 15L, TimeUnit.MINUTES);
        verify(redisTemplate).delete("email:verification:user@example.com:code");
        verify(redisTemplate).delete("email:verification:user@example.com:attempts");
    }

    @Test
    @DisplayName("verifyCode throws when max attempts exceeded")
    void verifyCode_MaxAttemptsExceeded_ThrowsTooManyRequests() {
        when(valueOperations.get("email:verification:user@example.com:attempts")).thenReturn("5");

        assertThatThrownBy(() -> emailTokenService.verifyCode("user@example.com", "123456"))
                .isInstanceOf(TooManyRequestsException.class)
                .hasMessage("Too many attempts. Please request a new code.");
    }

    @Test
    @DisplayName("verifyCode throws when code expired")
    void verifyCode_CodeExpired_ThrowsBadRequest() {
        when(valueOperations.get("email:verification:user@example.com:attempts")).thenReturn("0");
        when(valueOperations.get("email:verification:user@example.com:code")).thenReturn(null);

        assertThatThrownBy(() -> emailTokenService.verifyCode("user@example.com", "123456"))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Verification code expired or not found. Please request a new one.");
    }

    @Test
    @DisplayName("generateResetToken stores token in Redis")
    void generateResetToken_StoresTokenInRedis() {
        String token = emailTokenService.generateResetToken("user@example.com");

        assertThat(token).isNotBlank();
        verify(valueOperations).set(startsWith("email:reset:"), eq("user@example.com"), eq(1L), eq(TimeUnit.HOURS));
    }

    @Test
    @DisplayName("validateResetToken returns email for valid token")
    void validateResetToken_WithValidToken_ReturnsEmail() {
        when(valueOperations.get("email:reset:token-123")).thenReturn("user@example.com");

        String email = emailTokenService.validateResetToken("token-123");

        assertThat(email).isEqualTo("user@example.com");
    }

    @Test
    @DisplayName("validateResetToken throws for invalid token")
    void validateResetToken_WithInvalidToken_ThrowsBadRequest() {
        when(valueOperations.get("email:reset:token-123")).thenReturn(null);

        assertThatThrownBy(() -> emailTokenService.validateResetToken("token-123"))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Reset token is invalid or expired");
    }

    @Test
    @DisplayName("invalidateResetToken deletes Redis key")
    void invalidateResetToken_DeletesRedisKey() {
        emailTokenService.invalidateResetToken("token-123");

        verify(redisTemplate).delete("email:reset:token-123");
    }
}
