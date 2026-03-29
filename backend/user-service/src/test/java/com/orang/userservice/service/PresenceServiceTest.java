package com.orang.userservice.service;

import com.orang.shared.constants.PresenceConstants;
import com.orang.shared.presence.UserStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PresenceServiceTest {

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private SetOperations<String, String> setOperations;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private PresenceService presenceService;

    private static final String TEST_USER_ID = "844ec9f6-f781-4f67-aab0-1f33cf9734f7";

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForSet()).thenReturn(setOperations);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        presenceService = new PresenceService(redisTemplate);
    }

    @Nested
    @DisplayName("getUserStatus()")
    class GetUserStatusTests {

        @Test
        @DisplayName("returns OFFLINE when no sessions exist")
        void returnsOfflineWhenNoSessions() {
            // Arrange
            String sessionsKey = PresenceConstants.userSessionsKey(TEST_USER_ID);
            when(setOperations.size(sessionsKey)).thenReturn(0L);

            // Act
            UserStatus status = presenceService.getUserStatus(TEST_USER_ID);

            // Assert
            assertThat(status).isEqualTo(UserStatus.OFFLINE);
        }

        @Test
        @DisplayName("returns OFFLINE when sessions key is null")
        void returnsOfflineWhenSessionsKeyNull() {
            // Arrange
            String sessionsKey = PresenceConstants.userSessionsKey(TEST_USER_ID);
            when(setOperations.size(sessionsKey)).thenReturn(null);

            // Act
            UserStatus status = presenceService.getUserStatus(TEST_USER_ID);

            // Assert
            assertThat(status).isEqualTo(UserStatus.OFFLINE);
        }

        @Test
        @DisplayName("returns ONLINE when session exists and activity is recent")
        void returnsOnlineWhenActiveRecently() {
            // Arrange
            String sessionsKey = PresenceConstants.userSessionsKey(TEST_USER_ID);
            String activityKey = PresenceConstants.userLastActivityKey(TEST_USER_ID);

            // User has 1 session
            when(setOperations.size(sessionsKey)).thenReturn(1L);

            // Last activity was 60 seconds ago (< 2 min threshold)
            long recentTimestamp = Instant.now().getEpochSecond() - 60;
            when(valueOperations.get(activityKey)).thenReturn(String.valueOf(recentTimestamp));

            // Act
            UserStatus status = presenceService.getUserStatus(TEST_USER_ID);

            // Assert
            assertThat(status).isEqualTo(UserStatus.ONLINE);
        }

        @Test
        @DisplayName("returns AWAY when session exists but idle 2-10 minutes")
        void returnsAwayWhenIdleShortTime() {
            // Arrange
            String sessionsKey = PresenceConstants.userSessionsKey(TEST_USER_ID);
            String activityKey = PresenceConstants.userLastActivityKey(TEST_USER_ID);

            when(setOperations.size(sessionsKey)).thenReturn(1L);

            // Last activity was 5 minutes ago (300 seconds)
            long idleTimestamp = Instant.now().getEpochSecond() - 300;
            when(valueOperations.get(activityKey)).thenReturn(String.valueOf(idleTimestamp));

            // Act
            UserStatus status = presenceService.getUserStatus(TEST_USER_ID);

            // Assert
            assertThat(status).isEqualTo(UserStatus.AWAY);
        }

        @Test
        @DisplayName("returns OFFLINE when session exists but idle over 10 minutes")
        void returnsOfflineWhenIdleTooLong() {
            // Arrange
            String sessionsKey = PresenceConstants.userSessionsKey(TEST_USER_ID);
            String activityKey = PresenceConstants.userLastActivityKey(TEST_USER_ID);

            when(setOperations.size(sessionsKey)).thenReturn(1L);

            // Last activity was 12 minutes ago (720 seconds)
            long veryIdleTimestamp = Instant.now().getEpochSecond() - 720;
            when(valueOperations.get(activityKey)).thenReturn(String.valueOf(veryIdleTimestamp));

            // Act
            UserStatus status = presenceService.getUserStatus(TEST_USER_ID);

            // Assert
            assertThat(status).isEqualTo(UserStatus.OFFLINE);
        }

        @Test
        @DisplayName("returns OFFLINE when session exists but no activity record")
        void returnsOfflineWhenNoActivityRecord() {
            // Arrange
            String sessionsKey = PresenceConstants.userSessionsKey(TEST_USER_ID);
            String activityKey = PresenceConstants.userLastActivityKey(TEST_USER_ID);

            when(setOperations.size(sessionsKey)).thenReturn(1L);
            when(valueOperations.get(activityKey)).thenReturn(null);

            // Act
            UserStatus status = presenceService.getUserStatus(TEST_USER_ID);

            // Assert
            assertThat(status).isEqualTo(UserStatus.OFFLINE);
        }
    }

    @Nested
    @DisplayName("isUserOnline()")
    class IsUserOnlineTests {

        @Test
        @DisplayName("returns true when user is ONLINE")
        void returnsTrueWhenOnline() {
            // Arrange
            String sessionsKey = PresenceConstants.userSessionsKey(TEST_USER_ID);
            String activityKey = PresenceConstants.userLastActivityKey(TEST_USER_ID);

            when(setOperations.size(sessionsKey)).thenReturn(1L);
            long recentTimestamp = Instant.now().getEpochSecond() - 30;
            when(valueOperations.get(activityKey)).thenReturn(String.valueOf(recentTimestamp));

            // Act
            boolean isOnline = presenceService.isUserOnline(TEST_USER_ID);

            // Assert
            assertThat(isOnline).isTrue();
        }

        @Test
        @DisplayName("returns true when user is AWAY")
        void returnsTrueWhenAway() {
            // Arrange
            String sessionsKey = PresenceConstants.userSessionsKey(TEST_USER_ID);
            String activityKey = PresenceConstants.userLastActivityKey(TEST_USER_ID);

            when(setOperations.size(sessionsKey)).thenReturn(1L);
            long idleTimestamp = Instant.now().getEpochSecond() - 300;
            when(valueOperations.get(activityKey)).thenReturn(String.valueOf(idleTimestamp));

            // Act
            boolean isOnline = presenceService.isUserOnline(TEST_USER_ID);

            // Assert
            assertThat(isOnline).isTrue();
        }

        @Test
        @DisplayName("returns false when user is OFFLINE")
        void returnsFalseWhenOffline() {
            // Arrange
            String sessionsKey = PresenceConstants.userSessionsKey(TEST_USER_ID);
            when(setOperations.size(sessionsKey)).thenReturn(0L);

            // Act
            boolean isOnline = presenceService.isUserOnline(TEST_USER_ID);

            // Assert
            assertThat(isOnline).isFalse();
        }
    }

    @Nested
    @DisplayName("getBatchUserStatus()")
    class BatchStatusTests {

        @Test
        @DisplayName("returns status for multiple users")
        void returnsStatusForMultipleUsers() {
            // Arrange
            String user1 = "user-1";
            String user2 = "user-2";

            // User 1 is online
            when(setOperations.size(PresenceConstants.userSessionsKey(user1))).thenReturn(1L);
            when(valueOperations.get(PresenceConstants.userLastActivityKey(user1)))
                    .thenReturn(String.valueOf(Instant.now().getEpochSecond() - 30));

            // User 2 is offline
            when(setOperations.size(PresenceConstants.userSessionsKey(user2))).thenReturn(0L);

            // Act
            Map<String, UserStatus> result = presenceService.getBatchUserStatus(List.of(user1, user2));

            // Assert
            assertThat(result).hasSize(2);
            assertThat(result.get(user1)).isEqualTo(UserStatus.ONLINE);
            assertThat(result.get(user2)).isEqualTo(UserStatus.OFFLINE);
        }
    }

    @Nested
    @DisplayName("getLastActivityTime()")
    class LastActivityTests {

        @Test
        @DisplayName("returns timestamp when activity exists")
        void returnsTimestampWhenExists() {
            // Arrange
            String activityKey = PresenceConstants.userLastActivityKey(TEST_USER_ID);
            long expectedTimestamp = 1774451944L;
            when(valueOperations.get(activityKey)).thenReturn(String.valueOf(expectedTimestamp));

            // Act
            Long result = presenceService.getLastActivityTime(TEST_USER_ID);

            // Assert
            assertThat(result).isEqualTo(expectedTimestamp);
        }

        @Test
        @DisplayName("returns null when no activity record")
        void returnsNullWhenNoActivity() {
            // Arrange
            String activityKey = PresenceConstants.userLastActivityKey(TEST_USER_ID);
            when(valueOperations.get(activityKey)).thenReturn(null);

            // Act
            Long result = presenceService.getLastActivityTime(TEST_USER_ID);

            // Assert
            assertThat(result).isNull();
        }
    }

    @Nested
    @DisplayName("getSessionCount()")
    class SessionCountTests {

        @Test
        @DisplayName("returns correct session count")
        void returnsCorrectCount() {
            // Arrange
            String sessionsKey = PresenceConstants.userSessionsKey(TEST_USER_ID);
            when(setOperations.size(sessionsKey)).thenReturn(3L);

            // Act
            int count = presenceService.getSessionCount(TEST_USER_ID);

            // Assert
            assertThat(count).isEqualTo(3);
        }

        @Test
        @DisplayName("returns zero when no sessions")
        void returnsZeroWhenNoSessions() {
            // Arrange
            String sessionsKey = PresenceConstants.userSessionsKey(TEST_USER_ID);
            when(setOperations.size(sessionsKey)).thenReturn(null);

            // Act
            int count = presenceService.getSessionCount(TEST_USER_ID);

            // Assert
            assertThat(count).isEqualTo(0);
        }
    }

    @Nested
    @DisplayName("getActiveSessions()")
    class ActiveSessionsTests {

        @Test
        @DisplayName("returns set of session IDs")
        void returnsSessionIds() {
            // Arrange
            String sessionsKey = PresenceConstants.userSessionsKey(TEST_USER_ID);
            Set<String> expectedSessions = Set.of("session-1", "session-2");
            when(setOperations.members(sessionsKey)).thenReturn(expectedSessions);

            // Act
            Set<String> result = presenceService.getActiveSessions(TEST_USER_ID);

            // Assert
            assertThat(result).containsExactlyInAnyOrder("session-1", "session-2");
        }

        @Test
        @DisplayName("returns empty set when no sessions")
        void returnsEmptySetWhenNoSessions() {
            // Arrange
            String sessionsKey = PresenceConstants.userSessionsKey(TEST_USER_ID);
            when(setOperations.members(sessionsKey)).thenReturn(null);

            // Act
            Set<String> result = presenceService.getActiveSessions(TEST_USER_ID);

            // Assert
            assertThat(result).isEmpty();
        }
    }
}