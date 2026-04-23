package com.orang.authservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.orang.authservice.dto.AuthResponse;
import com.orang.authservice.dto.RegistrationResponse;
import com.orang.authservice.dto.RefreshRequest;
import com.orang.authservice.service.AuthService;
import com.orang.authservice.service.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private AuthService authService;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AuthController authController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();

        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders.standaloneSetup(authController)
                .setValidator(validator)
                .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
                .build();
    }

    @Test
    @DisplayName("POST /api/auth/register returns 201")
    void register_ReturnsCreated() throws Exception {
        RegistrationResponse response = RegistrationResponse.builder()
                .userId(UUID.randomUUID())
                .email("new@example.com")
                .displayName("New User")
                .emailVerified(false)
                .build();

        when(authService.register(any())).thenReturn(response);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "new@example.com",
                                  "password": "password123",
                                  "displayName": "New User"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("new@example.com"));
    }

    @Test
    @DisplayName("POST /api/auth/login returns 200")
    void login_ReturnsOk() throws Exception {
        AuthResponse response = AuthResponse.builder()
                .userId(UUID.randomUUID())
                .email("test@example.com")
                .displayName("Test User")
                .accessToken("access-token")
                .refreshToken("refresh-token")
                .tokenType("Bearer")
                .expiresIn(900)
                .emailVerified(true)
                .build();

        when(authService.login(any())).thenReturn(response);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "test@example.com",
                                  "password": "password123"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("access-token"));
    }

    @Test
    @DisplayName("POST /api/auth/logout returns 204 and blacklists user")
    void logout_ReturnsNoContent() throws Exception {
        UUID userId = UUID.randomUUID();
        when(jwtService.extractUserId("token-123")).thenReturn(userId);

        mockMvc.perform(post("/api/auth/logout")
                        .header("Authorization", "Bearer token-123"))
                .andExpect(status().isNoContent());

        verify(authService).blacklistUser(userId, 604800L);
    }

    @Test
    @DisplayName("POST /api/auth/refresh returns 200")
    void refreshToken_ReturnsOk() throws Exception {
        when(authService.refreshToken(any(RefreshRequest.class)))
                .thenReturn(AuthResponse.builder().accessToken("new-access").refreshToken("new-refresh").build());

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RefreshRequest("refresh-token"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("new-access"));
    }

    @Test
    @DisplayName("POST /api/auth/verify-email returns 200")
    void verifyEmail_ReturnsOk() throws Exception {
        when(authService.verifyEmail(any())).thenReturn(AuthResponse.builder().accessToken("a").refreshToken("r").build());

        mockMvc.perform(post("/api/auth/verify-email")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "test@example.com",
                                  "code": "123456"
                                }
                                """))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("POST /api/auth/resend-verification returns 200")
    void resendVerification_ReturnsOk() throws Exception {
        mockMvc.perform(post("/api/auth/resend-verification")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "test@example.com"
                                }
                                """))
                .andExpect(status().isOk());

        verify(authService).resendVerification("test@example.com");
    }

    @Test
    @DisplayName("POST /api/auth/forgot-password returns 200")
    void forgotPassword_ReturnsOk() throws Exception {
        mockMvc.perform(post("/api/auth/forgot-password")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "test@example.com"
                                }
                                """))
                .andExpect(status().isOk());

        verify(authService).requestPasswordReset("test@example.com");
    }

    @Test
    @DisplayName("GET /api/auth/reset-password/validate returns 200")
    void validateResetToken_ReturnsOk() throws Exception {
        mockMvc.perform(get("/api/auth/reset-password/validate").param("token", "abc-token"))
                .andExpect(status().isOk());

        verify(authService).validateResetToken("abc-token");
    }

    @Test
    @DisplayName("POST /api/auth/reset-password returns 200")
    void resetPassword_ReturnsOk() throws Exception {
        mockMvc.perform(post("/api/auth/reset-password")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "token": "abc-token",
                                  "newPassword": "new-password"
                                }
                                """))
                .andExpect(status().isOk());

        verify(authService).resetPassword("abc-token", "new-password");
    }

    @Test
    @DisplayName("POST /api/auth/register returns 400 for invalid payload")
    void register_WithInvalidPayload_ReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "bad",
                                  "password": "123",
                                  "displayName": ""
                                }
                                """))
                .andExpect(status().isBadRequest());
    }
}
