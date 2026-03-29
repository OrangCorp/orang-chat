package com.orang.chatservice.service;

import com.orang.shared.constants.PresenceConstants;
import com.orang.shared.presence.PresenceUtils;
import com.orang.shared.presence.UserStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class PresenceService {

    private final RedisTemplate<String, String> redisTemplate;
    private final PresenceUtils presenceUtils;

    public PresenceService(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
        this.presenceUtils = new PresenceUtils(redisTemplate);
    }

    public void addSession(String userId, String sessionId, Map<String, String> metadata) {
        String sessionsKey = PresenceConstants.userSessionsKey(userId);
        String sessionMetaKey = PresenceConstants.sessionMetaKey(sessionId);

        redisTemplate.opsForSet().add(sessionsKey, sessionId);
        redisTemplate.expire(sessionsKey, PresenceConstants.SESSION_TTL_SECONDS, TimeUnit.SECONDS);

        Map<String, String> meta = new HashMap<>();
        meta.put(PresenceConstants.META_USER_ID, userId);
        meta.put(PresenceConstants.META_CONNECTED_AT, String.valueOf(Instant.now().getEpochSecond()));
        meta.put(PresenceConstants.META_LAST_ACTIVE_AT, String.valueOf(Instant.now().getEpochSecond()));

        if (metadata != null) {
            meta.putAll(metadata);
        }

        redisTemplate.opsForHash().putAll(sessionMetaKey, meta);
        redisTemplate.expire(sessionMetaKey, PresenceConstants.SESSION_TTL_SECONDS, TimeUnit.SECONDS);

        updateLastActivity(userId);

        log.info("Session added: userId={}, sessionId={}", userId, sessionId);
    }

    public void removeSession(String userId, String sessionId) {
        String sessionsKey = PresenceConstants.userSessionsKey(userId);
        String sessionMetaKey = PresenceConstants.sessionMetaKey(sessionId);

        redisTemplate.opsForSet().remove(sessionsKey, sessionId);
        redisTemplate.delete(sessionMetaKey);

        Long remainingSessions = redisTemplate.opsForSet().size(sessionsKey);
        if (remainingSessions == null || remainingSessions == 0) {
            redisTemplate.delete(sessionsKey);
        }

        log.info("Session removed: userId={}, sessionId={}, remaining={}",
                userId, sessionId, remainingSessions);
    }

    public void updateLastActivity(String userId) {
        String activityKey = PresenceConstants.userLastActivityKey(userId);
        long timestamp = Instant.now().getEpochSecond();

        redisTemplate.opsForValue().set(
                activityKey,
                String.valueOf(timestamp),
                PresenceConstants.ACTIVITY_TTL_SECONDS,
                TimeUnit.SECONDS
        );
    }

    public void refreshSession(String userId, String sessionId) {
        String sessionsKey = PresenceConstants.userSessionsKey(userId);
        String sessionMetaKey = PresenceConstants.sessionMetaKey(sessionId);

        redisTemplate.expire(sessionsKey, PresenceConstants.SESSION_TTL_SECONDS, TimeUnit.SECONDS);
        redisTemplate.expire(sessionMetaKey, PresenceConstants.SESSION_TTL_SECONDS, TimeUnit.SECONDS);

        redisTemplate.opsForHash().put(
                sessionMetaKey,
                PresenceConstants.META_LAST_ACTIVE_AT,
                String.valueOf(Instant.now().getEpochSecond())
        );

        updateLastActivity(userId);
    }

    public boolean terminateSession(String sessionId) {
        Map<Object, Object> meta = presenceUtils.getSessionMetadata(sessionId);

        if (meta.isEmpty()) {
            return false;
        }

        String userId = (String) meta.get(PresenceConstants.META_USER_ID);
        if (userId != null) {
            removeSession(userId, sessionId);
            return true;
        }

        return false;
    }

    public UserStatus getUserStatus(String userId) {
        return presenceUtils.getUserStatus(userId);
    }

    public boolean isUserOnline(String userId) {
        return presenceUtils.isUserOnline(userId);
    }

    public Long getLastActivityTime(String userId) {
        return presenceUtils.getLastActivityTime(userId);
    }

    public Set<String> getActiveSessions(String userId) {
        return presenceUtils.getActiveSessions(userId);
    }

    public int getSessionCount(String userId) {
        return presenceUtils.getSessionCount(userId);
    }

    public Map<Object, Object> getSessionMetadata(String sessionId) {
        return presenceUtils.getSessionMetadata(sessionId);
    }

    public Map<String, UserStatus> getBatchUserStatus(List<String> userIds) {
        return presenceUtils.getBatchUserStatus(userIds);
    }
}