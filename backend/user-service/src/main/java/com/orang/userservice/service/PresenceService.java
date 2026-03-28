package com.orang.userservice.service;

import com.orang.shared.presence.PresenceUtils;
import com.orang.shared.presence.UserStatus;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Service for querying user presence/online status.
 *
 * This is a READ-ONLY service. Session management (add/remove)
 * happens in Chat Service.
 */
@Service
public class PresenceService {

    private final PresenceUtils presenceUtils;

    public PresenceService(RedisTemplate<String, String> redisTemplate) {
        this.presenceUtils = new PresenceUtils(redisTemplate);
    }

    /**
     * Gets a user's current status.
     *
     * @param userId the user ID
     * @return ONLINE, AWAY, or OFFLINE
     */
    public UserStatus getUserStatus(String userId) {
        return presenceUtils.getUserStatus(userId);
    }

    /**
     * Checks if user is online (ONLINE or AWAY).
     *
     * @param userId the user ID
     * @return true if user has active sessions
     */
    public boolean isUserOnline(String userId) {
        return presenceUtils.isUserOnline(userId);
    }

    /**
     * Gets status for multiple users at once.
     *
     * @param userIds list of user IDs
     * @return map of userId -> status
     */
    public Map<String, UserStatus> getBatchUserStatus(List<String> userIds) {
        return presenceUtils.getBatchUserStatus(userIds);
    }

    /**
     * Gets last activity timestamp for "Last seen X ago" feature.
     *
     * @param userId the user ID
     * @return epoch seconds, or null if unknown
     */
    public Long getLastActivityTime(String userId) {
        return presenceUtils.getLastActivityTime(userId);
    }

    /**
     * Gets all active session IDs for a user.
     *
     * @param userId the user ID
     * @return set of session IDs
     */
    public Set<String> getActiveSessions(String userId) {
        return presenceUtils.getActiveSessions(userId);
    }

    /**
     * Gets the number of active sessions.
     *
     * @param userId the user ID
     * @return session count
     */
    public int getSessionCount(String userId) {
        return presenceUtils.getSessionCount(userId);
    }

    /**
     * Gets metadata for a specific session.
     *
     * @param sessionId the session ID
     * @return metadata map (userId, connectedAt, userAgent, etc.)
     */
    public Map<Object, Object> getSessionMetadata(String sessionId) {
        return presenceUtils.getSessionMetadata(sessionId);
    }
}