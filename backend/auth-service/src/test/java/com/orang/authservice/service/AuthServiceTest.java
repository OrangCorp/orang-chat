package com.orang.authservice.service;

import com.orang.authservice.dto.AuthResponse;
import com.orang.authservice.dto.LoginRequest;
import com.orang.authservice.dto.RefreshRequest;
import com.orang.authservice.dto.RegisterRequest;
import com.orang.authservice.dto.RegistrationResponse;
import com.orang.authservice.dto.VerifyEmailRequest;
import com.orang.authservice.entity.User;
import com.orang.authservice.repository.UserRepository;
import com.orang.shared.exception.BadRequestException;
import com.orang.shared.exception.ForbiddenException;
import com.orang.shared.exception.ResourceNotFoundException;
import com.orang.shared.exception.UnauthorizedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)  // ✨ Fixes UnnecessaryStubbingException
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private RabbitTemplate rabbitTemplate;

    @Mock
    private JwtService jwtService;

        @Mock
        private EmailTokenService emailTokenService;

        @Mock
        private EmailService emailService;

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private AuthService authService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(UUID.randomUUID())
                .email("test@example.com")
                .passwordHash("hashed_password")
                .displayName("Test User")
                .build();

        // Set @Value fields via reflection
        ReflectionTestUtils.setField(authService, "jwtExpiration", 900000L);
        ReflectionTestUtils.setField(authService, "refreshExpiration", 604800000L);
        ReflectionTestUtils.setField(authService, "passwordResetBaseUrl", "http://localhost:3000/reset-password");

        // Setup Redis mock
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    // ==================== LOGIN TESTS ====================

    @Nested
    @DisplayName("Login Tests")
    class LoginTests {

        private LoginRequest loginRequest;

        @BeforeEach
        void setUp() {
            loginRequest = new LoginRequest();
            loginRequest.setEmail("test@example.com");
            loginRequest.setPassword("correct_password");
        }

        @Test
        @DisplayName("returns auth response with both tokens for valid credentials")
        void login_WithValidCredentials_ReturnsAuthResponse() {
            // Arrange
                        testUser.setEmailVerified(true);
            when(userRepository.findByEmail(loginRequest.getEmail()))
                    .thenReturn(Optional.of(testUser));
            when(passwordEncoder.matches(loginRequest.getPassword(), testUser.getPasswordHash()))
                    .thenReturn(true);
            when(jwtService.generateToken(any(UUID.class), anyString()))
                    .thenReturn("fake.access.token");
            when(jwtService.generateRefreshToken(any(UUID.class)))
                    .thenReturn("fake.refresh.token");

            // Act
            AuthResponse response = authService.login(loginRequest);

            // Assert
            assertThat(response).isNotNull();
            assertThat(response.getEmail()).isEqualTo("test@example.com");
            assertThat(response.getAccessToken()).isEqualTo("fake.access.token");
            assertThat(response.getRefreshToken()).isEqualTo("fake.refresh.token");
            assertThat(response.getTokenType()).isEqualTo("Bearer");
            assertThat(response.getExpiresIn()).isEqualTo(900L);
        }

        @Test
        @DisplayName("throws UnauthorizedException for non-existent email")
        void login_WithNonExistentEmail_ThrowsUnauthorizedException() {
            // Arrange
            when(userRepository.findByEmail("unknown@example.com"))
                    .thenReturn(Optional.empty());

            loginRequest.setEmail("unknown@example.com");

            // Act & Assert
            assertThatThrownBy(() -> authService.login(loginRequest))
                    .isInstanceOf(UnauthorizedException.class)
                    .hasMessage("Invalid email or password");
        }

        @Test
        @DisplayName("throws UnauthorizedException for wrong password")
        void login_WithWrongPassword_ThrowsUnauthorizedException() {
            // Arrange
            when(userRepository.findByEmail("test@example.com"))
                    .thenReturn(Optional.of(testUser));
            when(passwordEncoder.matches("wrong_password", "hashed_password"))
                    .thenReturn(false);

            loginRequest.setPassword("wrong_password");

            // Act & Assert
            assertThatThrownBy(() -> authService.login(loginRequest))
                    .isInstanceOf(UnauthorizedException.class)
                    .hasMessage("Invalid email or password");
        }

                @Test
                @DisplayName("throws ForbiddenException for unverified email")
                void login_WithUnverifiedEmail_ThrowsForbiddenException() {
                        // Arrange
                        testUser.setEmailVerified(false);
                        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
                        when(passwordEncoder.matches("correct_password", "hashed_password")).thenReturn(true);

                        // Act + Assert
                        assertThatThrownBy(() -> authService.login(loginRequest))
                                        .isInstanceOf(ForbiddenException.class)
                                        .hasMessage("Please verify your email before logging in");
                }
    }

        // ==================== REGISTER TESTS ====================

        @Nested
        @DisplayName("Register Tests")
        class RegisterTests {

                private RegisterRequest registerRequest;

                @BeforeEach
                void setUp() {
                        registerRequest = new RegisterRequest();
                        registerRequest.setEmail("NEW@Example.com");
                        registerRequest.setPassword("password123");
                        registerRequest.setDisplayName("New User");
                }

                @Test
                @DisplayName("saves user and sends verification email")
                void register_WithValidData_SavesUserAndSendsVerificationEmail() {
                        // Arrange
                        when(userRepository.existsByEmail("new@example.com")).thenReturn(false);
                        when(passwordEncoder.encode("password123")).thenReturn("encoded-password");
                        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
                                User user = invocation.getArgument(0);
                                user.setId(UUID.randomUUID());
                                return user;
                        });
                        when(emailTokenService.generateVerificationCode("new@example.com")).thenReturn("123456");

                        // Act
                                                RegistrationResponse response = authService.register(registerRequest);

                        // Assert
                        assertThat(response).isNotNull();
                        assertThat(response.getEmail()).isEqualTo("new@example.com");
                                                assertThat(response.isEmailVerified()).isFalse();
                            verify(rabbitTemplate).convertAndSend(
                                    eq("user.exchange"),
                                    eq("user.registered"),
                                    isA(com.orang.shared.event.UserRegisteredEvent.class)
                            );
                        verify(emailService).sendVerificationEmail("new@example.com", "New User", "123456");
                }

                @Test
                @DisplayName("throws BadRequestException for duplicate email")
                void register_WithDuplicateEmail_ThrowsBadRequestException() {
                        // Arrange
                        when(userRepository.existsByEmail("new@example.com")).thenReturn(true);

                        // Act + Assert
                        assertThatThrownBy(() -> authService.register(registerRequest))
                                        .isInstanceOf(BadRequestException.class)
                                        .hasMessage("Email already exists");

                        verify(userRepository, never()).save(any());
                        verify(emailTokenService, never()).generateVerificationCode(anyString());
                        verify(emailService, never()).sendVerificationEmail(anyString(), anyString(), anyString());
                }
        }

        // ==================== VERIFY EMAIL TESTS ====================

        @Nested
        @DisplayName("Verify Email Tests")
        class VerifyEmailTests {

                private VerifyEmailRequest request;

                @BeforeEach
                void setUp() {
                        request = new VerifyEmailRequest();
                        request.setEmail("Test@Example.com");
                        request.setCode("123456");
                        testUser.setEmailVerified(false);
                }

                @Test
                @DisplayName("verifies email and returns tokens")
                void verifyEmail_WithValidCode_ReturnsAuthResponse() {
                        // Arrange
                        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
                        when(emailTokenService.verifyCode("test@example.com", "123456")).thenReturn(true);
                        when(jwtService.generateToken(testUser.getId(), testUser.getEmail())).thenReturn("access");
                        when(jwtService.generateRefreshToken(testUser.getId())).thenReturn("refresh");

                        // Act
                        AuthResponse response = authService.verifyEmail(request);

                        // Assert
                        assertThat(response.getAccessToken()).isEqualTo("access");
                        assertThat(response.getRefreshToken()).isEqualTo("refresh");
                        verify(userRepository).save(argThat(user -> user.isEmailVerified()));
                }

                @Test
                @DisplayName("throws ResourceNotFoundException for missing user")
                void verifyEmail_WithMissingUser_ThrowsResourceNotFoundException() {
                        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.empty());

                        assertThatThrownBy(() -> authService.verifyEmail(request))
                                        .isInstanceOf(ResourceNotFoundException.class)
                                        .hasMessage("User not found");
                }

                @Test
                @DisplayName("throws BadRequestException when user already verified")
                void verifyEmail_WithAlreadyVerifiedUser_ThrowsBadRequestException() {
                        testUser.setEmailVerified(true);
                        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));

                        assertThatThrownBy(() -> authService.verifyEmail(request))
                                        .isInstanceOf(BadRequestException.class)
                                        .hasMessage("Email already verified");
                }

                @Test
                @DisplayName("throws BadRequestException for invalid code")
                void verifyEmail_WithInvalidCode_ThrowsBadRequestException() {
                        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
                        when(emailTokenService.verifyCode("test@example.com", "123456")).thenReturn(false);

                        assertThatThrownBy(() -> authService.verifyEmail(request))
                                        .isInstanceOf(BadRequestException.class)
                                        .hasMessage("Invalid verification code");
                }
        }

        // ==================== EMAIL FLOW TESTS ====================

        @Test
        @DisplayName("resendVerification sends code for unverified user")
        void resendVerification_WithUnverifiedUser_SendsVerificationEmail() {
                testUser.setEmailVerified(false);
                when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
                when(emailTokenService.generateVerificationCode("test@example.com")).thenReturn("222333");

                authService.resendVerification("TEST@example.com");

                verify(emailService).sendVerificationEmail("test@example.com", testUser.getDisplayName(), "222333");
        }

        @Test
        @DisplayName("resendVerification does nothing when user missing")
        void resendVerification_WithMissingUser_DoesNothing() {
                when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.empty());

                authService.resendVerification("test@example.com");

                verify(emailTokenService, never()).generateVerificationCode(anyString());
                verify(emailService, never()).sendVerificationEmail(anyString(), anyString(), anyString());
        }

        @Test
        @DisplayName("requestPasswordReset sends email for verified user")
        void requestPasswordReset_WithVerifiedUser_SendsPasswordResetEmail() {
                testUser.setEmailVerified(true);
                when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
                when(emailTokenService.generateResetToken("test@example.com")).thenReturn("reset-token");

                authService.requestPasswordReset("test@example.com");

                verify(emailService).sendPasswordResetEmail(
                                eq("test@example.com"),
                                eq(testUser.getDisplayName()),
                                eq("http://localhost:3000/reset-password?token=reset-token")
                );
        }

        @Test
        @DisplayName("requestPasswordReset does nothing for unverified user")
        void requestPasswordReset_WithUnverifiedUser_DoesNothing() {
                testUser.setEmailVerified(false);
                when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));

                authService.requestPasswordReset("test@example.com");

                verify(emailTokenService, never()).generateResetToken(anyString());
                verify(emailService, never()).sendPasswordResetEmail(anyString(), anyString(), anyString());
        }

        @Test
        @DisplayName("validateResetToken delegates to token service")
        void validateResetToken_DelegatesToEmailTokenService() {
                authService.validateResetToken("token-123");
                verify(emailTokenService).validateResetToken("token-123");
        }

        @Test
        @DisplayName("resetPassword updates hash and invalidates token")
        void resetPassword_WithValidToken_UpdatesPasswordAndInvalidatesToken() {
                testUser.setEmailVerified(true);
                when(emailTokenService.validateResetToken("token-123")).thenReturn("test@example.com");
                when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
                when(passwordEncoder.encode("new-password")).thenReturn("new-hash");

                authService.resetPassword("token-123", "new-password");

                verify(userRepository).save(argThat(user -> "new-hash".equals(user.getPasswordHash())));
                verify(emailTokenService).invalidateResetToken("token-123");
                verify(valueOperations).set(
                                eq("blacklist:user:" + testUser.getId()),
                                eq("revoked"),
                                eq(604800L),
                                eq(TimeUnit.SECONDS)
                );
        }

        @Test
        @DisplayName("resetPassword throws BadRequestException for missing user")
        void resetPassword_WithMissingUser_ThrowsBadRequestException() {
                when(emailTokenService.validateResetToken("token-123")).thenReturn("missing@example.com");
                when(userRepository.findByEmail("missing@example.com")).thenReturn(Optional.empty());

                assertThatThrownBy(() -> authService.resetPassword("token-123", "new-password"))
                                .isInstanceOf(BadRequestException.class)
                                .hasMessage("User not found");
        }

        @Test
        @DisplayName("blacklistUser stores key in Redis")
        void blacklistUser_StoresRedisKeyWithTtl() {
                UUID userId = UUID.randomUUID();

                authService.blacklistUser(userId, 123L);

                verify(valueOperations).set("blacklist:user:" + userId, "revoked", 123L, TimeUnit.SECONDS);
        }

        @Test
        @DisplayName("removeUserFromBlacklist deletes key")
        void removeUserFromBlacklist_DeletesRedisKey() {
                UUID userId = UUID.randomUUID();

                authService.removeUserFromBlacklist(userId);

                verify(redisTemplate).delete("blacklist:user:" + userId);
        }

    // ==================== REFRESH TESTS ====================

    @Nested
    @DisplayName("Refresh Token Tests")
    class RefreshTests {

        private RefreshRequest refreshRequest;
        private final String validRefreshToken = "valid.refresh.token";
        private final String tokenId = "token-jti-123";

        @BeforeEach
        void setUp() {
            refreshRequest = new RefreshRequest();
            refreshRequest.setRefreshToken(validRefreshToken);
        }

        @Test
        @DisplayName("returns new tokens for valid refresh token")
        void refresh_WithValidRefreshToken_ReturnsNewTokens() {
            // Arrange
            when(jwtService.isRefreshToken(validRefreshToken)).thenReturn(true);
            when(jwtService.extractUserId(validRefreshToken)).thenReturn(testUser.getId());
            when(jwtService.extractTokenId(validRefreshToken)).thenReturn(tokenId);
            when(redisTemplate.hasKey("blacklist:user:" + testUser.getId())).thenReturn(false);
            when(valueOperations.setIfAbsent(
                    eq("refresh:used:" + tokenId),
                    eq(testUser.getId().toString()),
                    anyLong(),
                    any(TimeUnit.class)
            )).thenReturn(true);  // First use — token NOT already used
            when(userRepository.findById(testUser.getId())).thenReturn(Optional.of(testUser));
            when(jwtService.generateToken(any(UUID.class), anyString())).thenReturn("new.access.token");
            when(jwtService.generateRefreshToken(any(UUID.class))).thenReturn("new.refresh.token");

            // Act
            AuthResponse response = authService.refreshToken(refreshRequest);

            // Assert
            assertThat(response.getAccessToken()).isEqualTo("new.access.token");
            assertThat(response.getRefreshToken()).isEqualTo("new.refresh.token");

            // Verify token was marked as used
            verify(valueOperations).setIfAbsent(
                    eq("refresh:used:" + tokenId),
                    eq(testUser.getId().toString()),
                    anyLong(),
                    any(TimeUnit.class)
            );
        }

        @Test
        @DisplayName("throws UnauthorizedException for invalid token")
        void refresh_WithInvalidToken_ThrowsUnauthorizedException() {
            // Arrange
            refreshRequest.setRefreshToken("invalid.token");
            when(jwtService.isRefreshToken("invalid.token")).thenReturn(false);

            // Act & Assert
            assertThatThrownBy(() -> authService.refreshToken(refreshRequest))
                    .isInstanceOf(UnauthorizedException.class)
                    .hasMessage("Invalid refresh token");
        }

        @Test
        @DisplayName("throws UnauthorizedException when access token used as refresh")
        void refresh_WithAccessToken_ThrowsUnauthorizedException() {
            // Arrange
            refreshRequest.setRefreshToken("access.token.here");
            when(jwtService.isRefreshToken("access.token.here")).thenReturn(false);

            // Act & Assert
            assertThatThrownBy(() -> authService.refreshToken(refreshRequest))
                    .isInstanceOf(UnauthorizedException.class)
                    .hasMessage("Invalid refresh token");
        }

        @Test
        @DisplayName("throws UnauthorizedException for blacklisted user")
        void refresh_WithBlacklistedUser_ThrowsUnauthorizedException() {
            // Arrange
            when(jwtService.isRefreshToken(validRefreshToken)).thenReturn(true);
            when(jwtService.extractUserId(validRefreshToken)).thenReturn(testUser.getId());
            when(jwtService.extractTokenId(validRefreshToken)).thenReturn(tokenId);
            when(redisTemplate.hasKey("blacklist:user:" + testUser.getId())).thenReturn(true);  // BLACKLISTED!

            // Act & Assert
            assertThatThrownBy(() -> authService.refreshToken(refreshRequest))
                    .isInstanceOf(UnauthorizedException.class)
                    .hasMessage("User session has been revoked");
        }

        @Test
        @DisplayName("throws UnauthorizedException for deleted user")
        void refresh_WithDeletedUser_ThrowsUnauthorizedException() {
            // Arrange
            UUID deletedUserId = UUID.randomUUID();
            when(jwtService.isRefreshToken(validRefreshToken)).thenReturn(true);
            when(jwtService.extractUserId(validRefreshToken)).thenReturn(deletedUserId);
            when(jwtService.extractTokenId(validRefreshToken)).thenReturn(tokenId);
            when(redisTemplate.hasKey("blacklist:user:" + deletedUserId)).thenReturn(false);
            when(valueOperations.setIfAbsent(
                    eq("refresh:used:" + tokenId),
                    eq(deletedUserId.toString()),
                    anyLong(),
                    any(TimeUnit.class)
            )).thenReturn(true);  // First use
            when(userRepository.findById(deletedUserId)).thenReturn(Optional.empty());  // User not found!

            // Act & Assert
            assertThatThrownBy(() -> authService.refreshToken(refreshRequest))
                    .isInstanceOf(UnauthorizedException.class)
                    .hasMessage("User not found");
        }

        @Test
        @DisplayName("detects token reuse and blacklists user")
        void refresh_WithReusedToken_BlacklistsUserAndThrows() {
            // Arrange
            when(jwtService.isRefreshToken(validRefreshToken)).thenReturn(true);
            when(jwtService.extractUserId(validRefreshToken)).thenReturn(testUser.getId());
            when(jwtService.extractTokenId(validRefreshToken)).thenReturn(tokenId);
            when(redisTemplate.hasKey("blacklist:user:" + testUser.getId())).thenReturn(false);
            when(valueOperations.setIfAbsent(
                    eq("refresh:used:" + tokenId),
                    anyString(),
                    anyLong(),
                    any(TimeUnit.class)
            )).thenReturn(false);  // Token ALREADY USED — reuse detected!

            // Act & Assert
            assertThatThrownBy(() -> authService.refreshToken(refreshRequest))
                    .isInstanceOf(UnauthorizedException.class)
                    .hasMessage("Token reuse detected - all sessions revoked");

            // Verify user was blacklisted
            verify(valueOperations).set(
                    eq("blacklist:user:" + testUser.getId()),
                    eq("revoked"),
                    anyLong(),
                    any(TimeUnit.class)
            );
        }

        @Test
        @DisplayName("marks token as used in Redis")
        void refresh_ShouldMarkTokenAsUsed() {
            // Arrange
            when(jwtService.isRefreshToken(validRefreshToken)).thenReturn(true);
            when(jwtService.extractUserId(validRefreshToken)).thenReturn(testUser.getId());
            when(jwtService.extractTokenId(validRefreshToken)).thenReturn(tokenId);
            when(redisTemplate.hasKey("blacklist:user:" + testUser.getId())).thenReturn(false);
            when(valueOperations.setIfAbsent(
                    eq("refresh:used:" + tokenId),
                    eq(testUser.getId().toString()),
                    anyLong(),
                    any(TimeUnit.class)
            )).thenReturn(true);  // First use
            when(userRepository.findById(testUser.getId())).thenReturn(Optional.of(testUser));
            when(jwtService.generateToken(any(), anyString())).thenReturn("new.access");
            when(jwtService.generateRefreshToken(any())).thenReturn("new.refresh");

            // Act
            authService.refreshToken(refreshRequest);

            // Assert — verify Redis setIfAbsent was called with correct key
            verify(valueOperations).setIfAbsent(
                    eq("refresh:used:" + tokenId),
                    eq(testUser.getId().toString()),
                    eq(604800L),  // 7 days in seconds
                    eq(TimeUnit.SECONDS)
            );
        }
    }
}