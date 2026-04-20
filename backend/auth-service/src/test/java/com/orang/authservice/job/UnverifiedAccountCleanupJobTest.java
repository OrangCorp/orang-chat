package com.orang.authservice.job;

import com.orang.authservice.entity.User;
import com.orang.authservice.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UnverifiedAccountCleanupJobTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UnverifiedAccountCleanupJob cleanupJob;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(cleanupJob, "unverifiedAccountTtlDays", 7);
    }

    @Test
    @DisplayName("cleanup skips delete when no users found")
    void cleanupUnverifiedAccounts_WhenEmpty_DoesNotDelete() {
        when(userRepository.findByEmailVerifiedFalseAndCreatedAtBefore(org.mockito.ArgumentMatchers.any(LocalDateTime.class)))
                .thenReturn(List.of());

        cleanupJob.cleanupUnverifiedAccounts();

        verify(userRepository, never()).deleteAll(org.mockito.ArgumentMatchers.anyList());
    }

    @Test
    @DisplayName("cleanup deletes users older than ttl")
    void cleanupUnverifiedAccounts_WhenUsersFound_DeletesAll() {
        User oldUnverified = User.builder()
                .id(UUID.randomUUID())
                .email("old@example.com")
                .passwordHash("hash")
                .displayName("Old")
                .emailVerified(false)
                .build();

        when(userRepository.findByEmailVerifiedFalseAndCreatedAtBefore(org.mockito.ArgumentMatchers.any(LocalDateTime.class)))
                .thenReturn(List.of(oldUnverified));

        cleanupJob.cleanupUnverifiedAccounts();

        verify(userRepository).deleteAll(List.of(oldUnverified));
    }
}
