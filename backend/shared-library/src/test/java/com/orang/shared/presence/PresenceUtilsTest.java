package com.orang.shared.presence;

import com.orang.shared.constants.PresenceConstants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PresenceUtilsTest {

    private RedisTemplate<String, String> redisTemplate;
    private SetOperations<String, String> setOperations;
    private ValueOperations<String, String> valueOperations;
    private HashOperations<String, String, Object> hashOperations;
    private PresenceUtils presenceUtils;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        redisTemplate = mock(RedisTemplate.class);
        setOperations = mock(SetOperations.class);
        valueOperations = mock(ValueOperations.class);
        hashOperations = mock(HashOperations.class);

        when(redisTemplate.opsForSet()).thenReturn(setOperations);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(redisTemplate.opsForHash()).thenReturn((HashOperations) hashOperations);

        presenceUtils = new PresenceUtils(redisTemplate);
    }

    @Test
    @DisplayName("returns OFFLINE when user has no active sessions")
    void getUserStatus_NoSessions_ReturnsOffline() {
        String userId = "user-1";
        when(setOperations.size(PresenceConstants.userSessionsKey(userId))).thenReturn(0L);

        assertThat(presenceUtils.getUserStatus(userId)).isEqualTo(UserStatus.OFFLINE);
    }

    @Test
    @DisplayName("returns OFFLINE when session count is null")
    void getUserStatus_NullSessionCount_ReturnsOffline() {
        String userId = "user-2";
        when(setOperations.size(PresenceConstants.userSessionsKey(userId))).thenReturn(null);

        assertThat(presenceUtils.getUserStatus(userId)).isEqualTo(UserStatus.OFFLINE);
    }

    @Test
    @DisplayName("returns OFFLINE when last activity is missing")
    void getUserStatus_MissingLastActivity_ReturnsOffline() {
        String userId = "user-3";
        when(setOperations.size(PresenceConstants.userSessionsKey(userId))).thenReturn(1L);
        when(valueOperations.get(PresenceConstants.userLastActivityKey(userId))).thenReturn(null);

        assertThat(presenceUtils.getUserStatus(userId)).isEqualTo(UserStatus.OFFLINE);
    }

    @Test
    @DisplayName("returns ONLINE when activity is recent")
    void getUserStatus_RecentActivity_ReturnsOnline() {
        String userId = "online-user";
        long now = Instant.now().getEpochSecond();
        when(setOperations.size(PresenceConstants.userSessionsKey(userId))).thenReturn(1L);
        when(valueOperations.get(PresenceConstants.userLastActivityKey(userId)))
                .thenReturn(String.valueOf(now - 30));

        assertThat(presenceUtils.getUserStatus(userId)).isEqualTo(UserStatus.ONLINE);
    }

    @Test
    @DisplayName("returns AWAY when user is idle beyond online threshold")
    void getUserStatus_AwayWindow_ReturnsAway() {
        String userId = "away-user";
        long now = Instant.now().getEpochSecond();
        when(setOperations.size(PresenceConstants.userSessionsKey(userId))).thenReturn(1L);
        when(valueOperations.get(PresenceConstants.userLastActivityKey(userId)))
                .thenReturn(String.valueOf(now - 300));

        assertThat(presenceUtils.getUserStatus(userId)).isEqualTo(UserStatus.AWAY);
    }

    @Test
    @DisplayName("returns OFFLINE when user is idle beyond away threshold")
    void getUserStatus_TooIdle_ReturnsOffline() {
        String userId = "offline-user";
        long now = Instant.now().getEpochSecond();
        when(setOperations.size(PresenceConstants.userSessionsKey(userId))).thenReturn(1L);
        when(valueOperations.get(PresenceConstants.userLastActivityKey(userId)))
                .thenReturn(String.valueOf(now - 2000));

        assertThat(presenceUtils.getUserStatus(userId)).isEqualTo(UserStatus.OFFLINE);
    }

    @Test
    @DisplayName("isUserOnline returns true for ONLINE and AWAY users")
    void isUserOnline_ForOnlineOrAway_ReturnsTrue() {
        long now = Instant.now().getEpochSecond();

        when(setOperations.size(PresenceConstants.userSessionsKey("online"))).thenReturn(1L);
        when(valueOperations.get(PresenceConstants.userLastActivityKey("online")))
                .thenReturn(String.valueOf(now - 1));
        when(setOperations.size(PresenceConstants.userSessionsKey("away"))).thenReturn(1L);
        when(valueOperations.get(PresenceConstants.userLastActivityKey("away")))
                .thenReturn(String.valueOf(now - 300));

        assertThat(presenceUtils.isUserOnline("online")).isTrue();
        assertThat(presenceUtils.isUserOnline("away")).isTrue();
    }

    @Test
    @DisplayName("isUserOnline returns false for OFFLINE user")
    void isUserOnline_ForOffline_ReturnsFalse() {
        when(setOperations.size(PresenceConstants.userSessionsKey("offline"))).thenReturn(0L);

        assertThat(presenceUtils.isUserOnline("offline")).isFalse();
    }

    @Test
    @DisplayName("getBatchUserStatus resolves every input user")
    void getBatchUserStatus_ReturnsStatusesForAllUsers() {
        long now = Instant.now().getEpochSecond();
        when(setOperations.size(PresenceConstants.userSessionsKey("u1"))).thenReturn(1L);
        when(valueOperations.get(PresenceConstants.userLastActivityKey("u1")))
                .thenReturn(String.valueOf(now - 10));
        when(setOperations.size(PresenceConstants.userSessionsKey("u2"))).thenReturn(0L);

        Map<String, UserStatus> statuses = presenceUtils.getBatchUserStatus(List.of("u1", "u2"));

        assertThat(statuses).containsEntry("u1", UserStatus.ONLINE);
        assertThat(statuses).containsEntry("u2", UserStatus.OFFLINE);
    }

    @Test
    @DisplayName("getLastActivityTime parses redis value or returns null")
    void getLastActivityTime_ReturnsParsedValueOrNull() {
        String userId = "u3";
        when(valueOperations.get(PresenceConstants.userLastActivityKey(userId))).thenReturn("123456");
        assertThat(presenceUtils.getLastActivityTime(userId)).isEqualTo(123456L);

        when(valueOperations.get(PresenceConstants.userLastActivityKey(userId))).thenReturn(null);
        assertThat(presenceUtils.getLastActivityTime(userId)).isNull();
    }

    @Test
    @DisplayName("getActiveSessions returns redis sessions or empty set")
    void getActiveSessions_ReturnsSessionsOrEmpty() {
        String userId = "u4";
        when(setOperations.members(PresenceConstants.userSessionsKey(userId))).thenReturn(Set.of("s1", "s2"));
        assertThat(presenceUtils.getActiveSessions(userId)).containsExactlyInAnyOrder("s1", "s2");

        when(setOperations.members(PresenceConstants.userSessionsKey(userId))).thenReturn(null);
        assertThat(presenceUtils.getActiveSessions(userId)).isEmpty();
    }

    @Test
    @DisplayName("getSessionCount handles null and non-null redis count")
    void getSessionCount_ReturnsIntCountOrZero() {
        String userId = "u5";
        when(setOperations.size(PresenceConstants.userSessionsKey(userId))).thenReturn(3L);
        assertThat(presenceUtils.getSessionCount(userId)).isEqualTo(3);

        when(setOperations.size(PresenceConstants.userSessionsKey(userId))).thenReturn(null);
        assertThat(presenceUtils.getSessionCount(userId)).isZero();
    }

    @Test
    @DisplayName("getSessionMetadata proxies hash entries from redis")
    void getSessionMetadata_ReturnsHashEntries() {
        Map<String, Object> metadata = Map.of("ipAddress", "127.0.0.1");
        when(hashOperations.entries(PresenceConstants.sessionMetaKey("session-1"))).thenReturn(metadata);

        assertThat(presenceUtils.getSessionMetadata("session-1")).isEqualTo(metadata);
    }
}
