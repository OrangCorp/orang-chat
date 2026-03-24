package com.orang.authservice.service;

import com.orang.authservice.dto.AuthResponse;
import com.orang.authservice.dto.LoginRequest;
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
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

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
    @DisplayName("login returns auth response for valid credentials")
    void login_WithValidCredentials_ReturnsAuthResponse() {
        when(userRepository.findByEmail(testLoginRequest.getEmail()))
                .thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches(testLoginRequest.getPassword(), testUser.getPasswordHash()))
                .thenReturn(true);
        when(jwtService.generateToken(any(UUID.class), anyString()))
                .thenReturn("fake.jwt.token");

        AuthResponse response = authService.login(testLoginRequest);

        assertThat(response).isNotNull();
        assertThat(response.getEmail()).isEqualTo("test@example.com");
        assertThat(response.getAccessToken()).isEqualTo("fake.jwt.token");
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
}
