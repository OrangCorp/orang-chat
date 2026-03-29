package com.orang.shared.constants;

public final class PresenceConstants {

    private PresenceConstants() {}

    // Redis key patterns
    public static final String USER_SESSIONS_KEY = "user:%s:sessions";
    public static final String USER_LAST_ACTIVITY_KEY = "user:%s:lastActivity";
    public static final String SESSION_META_KEY = "session:%s:meta";

    // Timeouts in seconds
    public static final long ONLINE_THRESHOLD_SECONDS = 120;      // 2 minutes
    public static final long AWAY_THRESHOLD_SECONDS = 600;        // 10 minutes
    public static final long SESSION_TTL_SECONDS = 900;           // 15 minutes
    public static final long ACTIVITY_TTL_SECONDS = 900;          // 15 minutes

    // Session metadata fields
    public static final String META_USER_ID = "userId";
    public static final String META_CONNECTED_AT = "connectedAt";
    public static final String META_LAST_ACTIVE_AT = "lastActiveAt";
    public static final String META_USER_AGENT = "userAgent";
    public static final String META_IP_ADDRESS = "ipAddress";

    // Helper methods for key generation
    public static String userSessionsKey(String userId) {
        return String.format(USER_SESSIONS_KEY, userId);
    }

    public static String userLastActivityKey(String userId) {
        return String.format(USER_LAST_ACTIVITY_KEY, userId);
    }

    public static String sessionMetaKey(String sessionId) {
        return String.format(SESSION_META_KEY, sessionId);
    }

}
