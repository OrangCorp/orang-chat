package com.orang.authservice.service;

import com.orang.authservice.dto.AuthResponse;
import com.orang.authservice.dto.LoginRequest;
import com.orang.authservice.dto.RefreshRequest;
import com.orang.authservice.entity.User;
import com.orang.authservice.repository.UserRepository;
import com.orang.shared.exception.UnauthorizedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private RabbitTemplate rabbitTemplate;

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AuthService authService;

    private User testUser;
    private LoginRequest testLoginRequest;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(UUID.randomUUID())
                .email("test@example.com")
                .passwordHash("hashed_password")
                .displayName("Test User")
                .build();

        testLoginRequest = new LoginRequest();
        testLoginRequest.setEmail("test@example.com");
        testLoginRequest.setPassword("correct_password");
    }

    @Test
    @DisplayName("login returns auth response with both access and refresh tokens")
    void login_WithValidCredentials_ReturnsAuthResponse() {
        // Arrange
        when(userRepository.findByEmail(testLoginRequest.getEmail()))
                .thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches(testLoginRequest.getPassword(), testUser.getPasswordHash()))
                .thenReturn(true);
        when(jwtService.generateToken(any(UUID.class), anyString()))
                .thenReturn("fake.access.token");
        when(jwtService.generateRefreshToken(any(UUID.class)))
                .thenReturn("fake.refresh.token");

        // Act
        AuthResponse response = authService.login(testLoginRequest);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getEmail()).isEqualTo("test@example.com");
        assertThat(response.getAccessToken()).isEqualTo("fake.access.token");
        assertThat(response.getRefreshToken()).isEqualTo("fake.refresh.token");
        assertThat(response.getTokenType()).isEqualTo("Bearer");
    }

    @Test
    @DisplayName("login throws UnauthorizedException for non-existent email")
    void login_WithNonExistentEmail_ThrowsUnauthorizedException() {
        // Arrange: User not found
        when(userRepository.findByEmail("unknown@example.com"))
                .thenReturn(Optional.empty());

        testLoginRequest.setEmail("unknown@example.com");

        // Act & Assert: Verify exception is thrown
        assertThatThrownBy(() -> authService.login(testLoginRequest))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("Invalid email or password");
    }

    @Test
    @DisplayName("login throws UnauthorizedException for wrong password")
    void login_WithWrongPassword_ThrowsUnauthorizedException() {
        // Arrange: User exists but password doesn't match
        when(userRepository.findByEmail("test@example.com"))
                .thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("wrong_password", "hashed_password"))
                .thenReturn(false);  // Password doesn't match!

        testLoginRequest.setPassword("wrong_password");

        // Act & Assert
        assertThatThrownBy(() -> authService.login(testLoginRequest))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("Invalid email or password");
    }

    @Test
    @DisplayName("refresh returns new tokens for valid refresh token")
    void refresh_WithValidRefreshToken_ReturnsNewTokens() {
        // Arrange
        RefreshRequest request = new RefreshRequest();
        request.setRefreshToken("valid.refresh.token");

        when(jwtService.isRefreshToken("valid.refresh.token")).thenReturn(true);
        when(jwtService.extractUserId("valid.refresh.token")).thenReturn(testUser.getId());
        when(userRepository.findById(testUser.getId())).thenReturn(Optional.of(testUser));
        when(jwtService.generateToken(any(UUID.class), anyString())).thenReturn("new.access.token");
        when(jwtService.generateRefreshToken(any(UUID.class))).thenReturn("new.refresh.token");

        // Act
        AuthResponse response = authService.refreshToken(request);

        // Assert
        assertThat(response.getAccessToken()).isEqualTo("new.access.token");
        assertThat(response.getRefreshToken()).isEqualTo("new.refresh.token");
    }

    @Test
    @DisplayName("refresh throws UnauthorizedException for invalid refresh token")
    void refresh_WithInvalidToken_ThrowsUnauthorizedException() {
        // Arrange
        RefreshRequest request = new RefreshRequest();
        request.setRefreshToken("invalid.token");

        when(jwtService.isRefreshToken("invalid.token")).thenReturn(false);

        // Act & Assert
        assertThatThrownBy(() -> authService.refreshToken(request))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("Invalid refresh token");
    }

    @Test
    @DisplayName("refresh throws UnauthorizedException for access token used as refresh")
    void refresh_WithAccessToken_ThrowsUnauthorizedException() {
        // Arrange
        RefreshRequest request = new RefreshRequest();
        request.setRefreshToken("valid.access.token");

        // isRefreshToken returns false for access tokens
        when(jwtService.isRefreshToken("valid.access.token")).thenReturn(false);

        // Act & Assert
        assertThatThrownBy(() -> authService.refreshToken(request))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("Invalid refresh token");
    }

    @Test
    @DisplayName("refresh throws UnauthorizedException for deleted user")
    void refresh_WithDeletedUser_ThrowsUnauthorizedException() {
        // Arrange
        RefreshRequest request = new RefreshRequest();
        request.setRefreshToken("valid.refresh.token");
        UUID deletedUserId = UUID.randomUUID();

        when(jwtService.isRefreshToken("valid.refresh.token")).thenReturn(true);
        when(jwtService.extractUserId("valid.refresh.token")).thenReturn(deletedUserId);
        when(userRepository.findById(deletedUserId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> authService.refreshToken(request))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("User not found");
    }

    @Test
    @DisplayName("refresh throws UnauthorizedException for blacklisted user")
    void refresh_WithBlacklistedUser_ThrowsUnauthorizedException() {
        // Arrange
        RefreshRequest request = new RefreshRequest();
        request.setRefreshToken("valid.refresh.token");

        when(jwtService.isRefreshToken("valid.refresh.token")).thenReturn(true);
        when(jwtService.extractUserId("valid.refresh.token")).thenReturn(testUser.getId());

        // Mock Redis to return true for hasKey (user is blacklisted)
        when(redisTemplate.hasKey("blacklist:user:" + testUser.getId())).thenReturn(true);

        // Act & Assert
        assertThatThrownBy(() -> authService.refreshToken(request))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("User session has been revoked");
    }
}
