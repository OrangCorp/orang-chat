package com.orang.authservice.service;

import com.orang.authservice.dto.AuthResponse;
import com.orang.authservice.dto.LoginRequest;
import com.orang.authservice.dto.RefreshRequest;
import com.orang.authservice.dto.RegisterRequest;
import com.orang.authservice.entity.User;
import com.orang.authservice.repository.UserRepository;
import com.orang.shared.event.UserRegisteredEvent;
import com.orang.shared.exception.BadRequestException;
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

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RabbitTemplate rabbitTemplate;
    private final RedisTemplate<String, String> redisTemplate;

    @Value("${jwt.expiration}")
    private long jwtExpiration;

    @Value("${jwt.refresh-expiration}")
    private long refreshExpiration;

    @Transactional
    public AuthResponse register(RegisterRequest registerRequest){
        if (userRepository.existsByEmail(registerRequest.getEmail())) {
            throw new BadRequestException("Email already exists");
        }

        User user = User.builder()
                .email(registerRequest.getEmail())
                .passwordHash(passwordEncoder.encode(registerRequest.getPassword()))
                .displayName(registerRequest.getDisplayName())
                .build();

        User savedUser = userRepository.save(user);
        UserRegisteredEvent userEvent = UserRegisteredEvent.builder()
                .userId(savedUser.getId())
                .displayName(savedUser.getDisplayName())
                .build();

        publishUserRegisteredEventAfterCommit(userEvent);

        String accessToken = jwtService.generateToken(savedUser.getId(), savedUser.getEmail());
        String refreshToken = jwtService.generateRefreshToken(savedUser.getId());
        return buildAuthResponse(savedUser, accessToken, refreshToken);
    }

    public AuthResponse login(LoginRequest loginRequest){
        User user = userRepository.findByEmail(loginRequest.getEmail())
                .orElseThrow(() -> new UnauthorizedException("Invalid email or password"));

        if (!passwordEncoder.matches(loginRequest.getPassword(), user.getPasswordHash())) {
            throw new UnauthorizedException("Invalid email or password");
        }

        String accessToken = jwtService.generateToken(user.getId(), user.getEmail());
        String refreshToken = jwtService.generateRefreshToken(user.getId());
        return buildAuthResponse(user, accessToken, refreshToken);
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
                java.util.concurrent.TimeUnit.SECONDS);

        return Boolean.FALSE.equals(wasSet);
    }

    private boolean isTokenAlreadyUsed(String tokenId) {
        String tokenKey = "refresh:used:" + tokenId;
        return redisTemplate.hasKey(tokenKey);
    }

    public void blacklistUser(UUID userId, long ttlSeconds) {
        String blacklistKey = "blacklist:user:" + userId;
        redisTemplate.opsForValue().set(blacklistKey, "revoked", ttlSeconds, java.util.concurrent.TimeUnit.SECONDS);
    }

    public void removeUserFromBlacklist(UUID userId) {
        String blacklistKey = "blacklist:user:" + userId;
        redisTemplate.delete(blacklistKey);
    }
}
