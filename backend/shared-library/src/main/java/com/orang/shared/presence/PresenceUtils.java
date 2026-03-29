package com.orang.shared.presence;

import com.orang.shared.constants.PresenceConstants;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;

import java.time.Instant;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@RequiredArgsConstructor
public class PresenceUtils {

    private final RedisTemplate<String, String> redisTemplate;

    public UserStatus getUserStatus(String userId) {
        String sessionsKey = PresenceConstants.userSessionsKey(userId);
        Long sessionCount = redisTemplate.opsForSet().size(sessionsKey);

        if (sessionCount == null || sessionCount == 0) {
            return UserStatus.OFFLINE;
        }

        String activityKey = PresenceConstants.userLastActivityKey(userId);
        String lastActivityStr = redisTemplate.opsForValue().get(activityKey);

        if (lastActivityStr == null) {
            return UserStatus.OFFLINE;
        }

        long lastActivity = Long.parseLong(lastActivityStr);
        long now = Instant.now().getEpochSecond();
        long idleSeconds = now - lastActivity;

        if (idleSeconds < PresenceConstants.ONLINE_THRESHOLD_SECONDS) {
            return UserStatus.ONLINE;
        } else if (idleSeconds < PresenceConstants.AWAY_THRESHOLD_SECONDS) {
            return UserStatus.AWAY;
        } else {
            return UserStatus.OFFLINE;
        }
    }

    public boolean isUserOnline(String userId) {
        UserStatus status = getUserStatus(userId);
        return status == UserStatus.ONLINE || status == UserStatus.AWAY;
    }

    // Can optimize this method by using Redis pipelines
    public Map<String, UserStatus> getBatchUserStatus(List<String> userIds) {
        Map<String, UserStatus> result = new HashMap<>();

        for (String userId : userIds) {
            result.put(userId, getUserStatus(userId));
        }

        return result;
    }

    public Long getLastActivityTime(String userId) {
        String activityKey = PresenceConstants.userLastActivityKey(userId);
        String lastActivityStr = redisTemplate.opsForValue().get(activityKey);

        if (lastActivityStr == null) {
            return null;
        }

        return Long.parseLong(lastActivityStr);
    }

    public Set<String> getActiveSessions(String userId) {
        String sessionsKey = PresenceConstants.userSessionsKey(userId);
        Set<String> sessions = redisTemplate.opsForSet().members(sessionsKey);
        return sessions != null ? sessions : Set.of();
    }

    public int getSessionCount(String userId) {
        String sessionsKey = PresenceConstants.userSessionsKey(userId);
        Long count = redisTemplate.opsForSet().size(sessionsKey);
        return count != null ? count.intValue() : 0;
    }

    public Map<Object, Object> getSessionMetadata(String sessionId) {
        String sessionMetaKey = PresenceConstants.sessionMetaKey(sessionId);
        return redisTemplate.opsForHash().entries(sessionMetaKey);
    }
}