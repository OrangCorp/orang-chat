package com.orang.authservice.service;

import com.orang.authservice.dto.AuthResponse;
import com.orang.authservice.dto.LoginRequest;
import com.orang.authservice.dto.RefreshRequest;
import com.orang.authservice.entity.User;
import com.orang.authservice.repository.UserRepository;
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