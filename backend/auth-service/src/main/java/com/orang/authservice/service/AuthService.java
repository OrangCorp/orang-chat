package com.orang.authservice.service;

import com.orang.authservice.dto.*;
import com.orang.authservice.entity.User;
import com.orang.authservice.repository.UserRepository;
import com.orang.shared.event.UserRegisteredEvent;
import com.orang.shared.exception.BadRequestException;
import com.orang.shared.exception.ForbiddenException;
import com.orang.shared.exception.ResourceNotFoundException;
import com.orang.shared.exception.UnauthorizedException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RabbitTemplate rabbitTemplate;
    private final RedisTemplate<String, String> redisTemplate;
    private final EmailTokenService emailTokenService;
    private final EmailService emailService;

    @Value("${jwt.expiration}")
    private long jwtExpiration;

    @Value("${jwt.refresh-expiration}")
    private long refreshExpiration;

    @Value("${app.password-reset.reset-url}")
    private String passwordResetBaseUrl;

    @Transactional
    public RegistrationResponse register(RegisterRequest registerRequest){
        if (userRepository.existsByEmail(registerRequest.getEmail().toLowerCase())) {
            throw new BadRequestException("Invalid email or password");
        }

        User user = User.builder()
                .email(registerRequest.getEmail().toLowerCase())
                .passwordHash(passwordEncoder.encode(registerRequest.getPassword()))
                .displayName(registerRequest.getDisplayName())
                .emailVerified(false)
                .build();

        User savedUser = userRepository.save(user);

        UserRegisteredEvent userEvent = UserRegisteredEvent.builder()
                .userId(savedUser.getId())
                .displayName(savedUser.getDisplayName())
                .build();

        publishUserRegisteredEventAfterCommit(userEvent);

        String code = emailTokenService.generateVerificationCode(savedUser.getEmail());
        emailService.sendVerificationEmail(
                savedUser.getEmail(),
                savedUser.getDisplayName(),
                code
        );

        return buildPendingVerificationResponse(savedUser);
    }

    private RegistrationResponse buildPendingVerificationResponse(User user) {
        return RegistrationResponse.builder()
                .userId(user.getId())
                .email(user.getEmail())
                .displayName(user.getDisplayName())
                .emailVerified(false)
                .build();
    }

    public AuthResponse login(LoginRequest loginRequest){
        User user = userRepository.findByEmail(loginRequest.getEmail().toLowerCase())
                .orElseThrow(() -> new UnauthorizedException("Invalid email or password"));

        if (!passwordEncoder.matches(loginRequest.getPassword(), user.getPasswordHash())) {
            throw new UnauthorizedException("Invalid email or password");
        }

        if (!user.isEmailVerified()) {
            throw new ForbiddenException("Please verify your email before logging in");
        }

        String accessToken = jwtService.generateToken(user.getId(), user.getEmail());
        String refreshToken = jwtService.generateRefreshToken(user.getId());
        return buildAuthResponse(user, accessToken, refreshToken);
    }

    @Transactional
    public AuthResponse verifyEmail(VerifyEmailRequest request) {
        String email = request.getEmail().toLowerCase();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (user.isEmailVerified()) {
            throw new BadRequestException("Email already verified");
        }

        boolean valid = emailTokenService.verifyCode(email, request.getCode());

        if (!valid) {
            throw new BadRequestException("Invalid verification code");
        }

        user.setEmailVerified(true);
        userRepository.save(user);

        String accessToken = jwtService.generateToken(user.getId(), user.getEmail());
        String refreshToken = jwtService.generateRefreshToken(user.getId());

        log.info("Email verified for user: {}", user.getId());

        return buildAuthResponse(user, accessToken, refreshToken);
    }

    public void resendVerification(String email) {
        String normalizedEmail = email.toLowerCase();
        User user = userRepository.findByEmail(normalizedEmail).orElse(null);

        if (user == null || user.isEmailVerified()) {
            return;
        }

        String code = emailTokenService.generateVerificationCode(normalizedEmail);
        emailService.sendVerificationEmail(
                normalizedEmail,
                user.getDisplayName(),
                code
        );
    }

    public void requestPasswordReset(String email) {
        String normalizedEmail = email.toLowerCase();
        User user = userRepository.findByEmail(normalizedEmail).orElse(null);

        if (user == null || !user.isEmailVerified()) {
            return;
        }

        String token = emailTokenService.generateResetToken(normalizedEmail);
        String resetUrl = passwordResetBaseUrl + "?token=" + token;

        emailService.sendPasswordResetEmail(
                normalizedEmail,
                user.getDisplayName(),
                resetUrl
        );

        log.info("Password reset email sent for user: {}", user.getId());
    }

    public void validateResetToken(String token) {
        emailTokenService.validateResetToken(token);
    }

    @Transactional
    public void resetPassword(String token, String newPassword) {
        String email = emailTokenService.validateResetToken(token);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BadRequestException("User not found"));

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        emailTokenService.invalidateResetToken(token);

        // Force re-login on all devices
        blacklistUser(user.getId(), refreshExpiration / 1000);

        log.info("Password reset completed for user: {}", user.getId());
    }


    public AuthResponse refreshToken(RefreshRequest request) {
        String refreshToken = request.getRefreshToken();
        if (!jwtService.isRefreshToken(refreshToken)) {
            throw new UnauthorizedException("Invalid refresh token");
        }

        UUID userId = jwtService.extractUserId(refreshToken);
        String tokenId = jwtService.extractTokenId(refreshToken);

        if (isUserBlacklisted(userId)) {
            throw new UnauthorizedException("User session has been revoked");
        }

        boolean tokenWasAlreadyUsed = markTokenAsUsed(
                tokenId,
                userId,
                refreshExpiration / 1000
        );

        if (tokenWasAlreadyUsed) {
            blacklistUser(userId, refreshExpiration / 1000);
            log.error("SECURITY: Refresh token reuse detected for user {}, token {}", userId, tokenId);
            throw new UnauthorizedException("Token reuse detected - all sessions revoked");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UnauthorizedException("User not found"));

        String newAccessToken = jwtService.generateToken(user.getId(), user.getEmail());
        String newRefreshToken = jwtService.generateRefreshToken(user.getId());

        return buildAuthResponse(user, newAccessToken, newRefreshToken);
    }

    private void publishUserRegisteredEventAfterCommit(UserRegisteredEvent userEvent) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    rabbitTemplate.convertAndSend(
                            "user.exchange",
                            "user.registered",
                            userEvent
                    );
                }
            });
        } else {
            rabbitTemplate.convertAndSend(
                    "user.exchange",
                    "user.registered",
                    userEvent
            );
        }
    }

    private AuthResponse buildAuthResponse(User user, String accessToken, String refreshToken) {
        return AuthResponse.builder()
                .userId(user.getId())
                .email(user.getEmail())
                .displayName(user.getDisplayName())
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtExpiration / 1000)
                .emailVerified(user.isEmailVerified())
                .build();
    }

    private boolean isUserBlacklisted(UUID userId) {
        String blacklistKey = "blacklist:user:" + userId;
        return redisTemplate.hasKey(blacklistKey);
    }

    private boolean markTokenAsUsed(String tokenId, UUID userId, long ttlSeconds) {
        String tokenKey = "refresh:used:" + tokenId;
        Boolean wasSet = redisTemplate.opsForValue().setIfAbsent(
                tokenKey,
                userId.toString(),
                ttlSeconds,
                TimeUnit.SECONDS);
        return Boolean.FALSE.equals(wasSet);
    }

    public void blacklistUser(UUID userId, long ttlSeconds) {
        String blacklistKey = "blacklist:user:" + userId;
        redisTemplate.opsForValue().set(blacklistKey, "revoked", ttlSeconds, TimeUnit.SECONDS);
    }

    public void removeUserFromBlacklist(UUID userId) {
        String blacklistKey = "blacklist:user:" + userId;
        redisTemplate.delete(blacklistKey);
    }
}
