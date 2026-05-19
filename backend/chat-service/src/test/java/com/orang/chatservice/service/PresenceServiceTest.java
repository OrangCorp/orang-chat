package com.orang.chatservice.service;

import com.orang.shared.constants.PresenceConstants;
import com.orang.shared.presence.PresenceUtils;
import com.orang.shared.presence.UserStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PresenceServiceTest {

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private SetOperations<String, String> setOperations;

    @Mock
    private HashOperations<String, Object, Object> hashOperations;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private PresenceService presenceService;

    private String userId;
    private String sessionId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID().toString();
        sessionId = "session-" + UUID.randomUUID();

        lenient().when(redisTemplate.opsForSet()).thenReturn(setOperations);
        lenient().when(redisTemplate.opsForHash()).thenReturn(hashOperations);
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        presenceService = new PresenceService(redisTemplate);
    }

    @Test
    void addSessionAddsSessionAndMetadata() {
        Map<String, String> metadata = new HashMap<>();
        metadata.put("device", "web");

        presenceService.addSession(userId, sessionId, metadata);

        String sessionsKey = PresenceConstants.userSessionsKey(userId);
        String sessionMetaKey = PresenceConstants.sessionMetaKey(sessionId);

        verify(setOperations).add(sessionsKey, sessionId);
        verify(redisTemplate).expire(sessionsKey, PresenceConstants.SESSION_TTL_SECONDS, TimeUnit.SECONDS);

        ArgumentCaptor<Map<String, Object>> metaCaptor = ArgumentCaptor.forClass(Map.class);
        verify(hashOperations).putAll(eq(sessionMetaKey), metaCaptor.capture());
        Map<String, Object> capturedMeta = metaCaptor.getValue();

        assertEquals(userId, capturedMeta.get(PresenceConstants.META_USER_ID));
        assertTrue(capturedMeta.containsKey(PresenceConstants.META_CONNECTED_AT));
        assertTrue(capturedMeta.containsKey(PresenceConstants.META_LAST_ACTIVE_AT));
        assertEquals("web", capturedMeta.get("device"));
    }

    @Test
    void removeSessionRemovesSessionAndMetadata() {
        presenceService.removeSession(userId, sessionId);

        String sessionsKey = PresenceConstants.userSessionsKey(userId);
        String sessionMetaKey = PresenceConstants.sessionMetaKey(sessionId);

        verify(setOperations).remove(sessionsKey, sessionId);
        verify(redisTemplate).delete(sessionMetaKey);
    }

    @Test
    void updateLastActivitySetsActivityTimestamp() {
        presenceService.updateLastActivity(userId);

        String activityKey = PresenceConstants.userLastActivityKey(userId);

        ArgumentCaptor<String> valueCaptor = ArgumentCaptor.forClass(String.class);
        verify(valueOperations).set(
                eq(activityKey),
                valueCaptor.capture(),
                eq(PresenceConstants.ACTIVITY_TTL_SECONDS),
                eq(TimeUnit.SECONDS)
        );

        long timestamp = Long.parseLong(valueCaptor.getValue());
        long now = Instant.now().getEpochSecond();
        assertTrue(Math.abs(timestamp - now) <= 1, "Activity timestamp should be current");
    }

    @Test
    void refreshSessionExtendsSessionTTL() {
        presenceService.refreshSession(userId, sessionId);

        String sessionsKey = PresenceConstants.userSessionsKey(userId);
        String sessionMetaKey = PresenceConstants.sessionMetaKey(sessionId);

        verify(redisTemplate, times(2)).expire(
                anyString(),
                eq(PresenceConstants.SESSION_TTL_SECONDS),
                eq(TimeUnit.SECONDS)
        );

        ArgumentCaptor<String> metaKeyCaptor = ArgumentCaptor.forClass(String.class);
        verify(hashOperations).put(
                metaKeyCaptor.capture(),
                eq(PresenceConstants.META_LAST_ACTIVE_AT),
                anyString()
        );
    }

    @Test
    void terminateSessionRemovesSessionWhenMetadataExists() {
        Map<Object, Object> metadata = new HashMap<>();
        metadata.put(PresenceConstants.META_USER_ID, userId);

        // Mock PresenceUtils to return metadata
        PresenceUtils presenceUtils = mock(PresenceUtils.class);
        when(presenceUtils.getSessionMetadata(sessionId)).thenReturn(metadata);
        ReflectionTestUtils.setField(presenceService, "presenceUtils", presenceUtils);

        boolean result = presenceService.terminateSession(sessionId);

        assertTrue(result);
        verify(setOperations).remove(PresenceConstants.userSessionsKey(userId), sessionId);
    }

    @Test
    void terminateSessionReturnsFalseWhenMetadataEmpty() {
        PresenceUtils presenceUtils = mock(PresenceUtils.class);
        when(presenceUtils.getSessionMetadata(sessionId)).thenReturn(new HashMap<>());
        ReflectionTestUtils.setField(presenceService, "presenceUtils", presenceUtils);

        boolean result = presenceService.terminateSession(sessionId);

        assertFalse(result);
    }

    @Test
    void getUserStatusDelegatesToPresenceUtils() {
        PresenceUtils presenceUtils = mock(PresenceUtils.class);
        UserStatus expectedStatus = UserStatus.ONLINE;
        when(presenceUtils.getUserStatus(userId)).thenReturn(expectedStatus);
        ReflectionTestUtils.setField(presenceService, "presenceUtils", presenceUtils);

        UserStatus result = presenceService.getUserStatus(userId);

        assertEquals(expectedStatus, result);
        verify(presenceUtils).getUserStatus(userId);
    }

    @Test
    void isUserOnlineDelegatesToPresenceUtils() {
        PresenceUtils presenceUtils = mock(PresenceUtils.class);
        when(presenceUtils.isUserOnline(userId)).thenReturn(true);
        ReflectionTestUtils.setField(presenceService, "presenceUtils", presenceUtils);

        boolean result = presenceService.isUserOnline(userId);

        assertTrue(result);
        verify(presenceUtils).isUserOnline(userId);
    }

    @Test
    void getActiveSessions() {
        PresenceUtils presenceUtils = mock(PresenceUtils.class);
        Set<String> expectedSessions = Set.of(sessionId, "session-2");
        when(presenceUtils.getActiveSessions(userId)).thenReturn(expectedSessions);
        ReflectionTestUtils.setField(presenceService, "presenceUtils", presenceUtils);

        Set<String> result = presenceService.getActiveSessions(userId);

        assertEquals(expectedSessions, result);
    }

    @Test
    void getSessionCount() {
        PresenceUtils presenceUtils = mock(PresenceUtils.class);
        when(presenceUtils.getSessionCount(userId)).thenReturn(2);
        ReflectionTestUtils.setField(presenceService, "presenceUtils", presenceUtils);

        int result = presenceService.getSessionCount(userId);

        assertEquals(2, result);
    }
}
