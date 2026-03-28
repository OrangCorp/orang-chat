package com.orang.shared.presence;

public enum UserStatus {
    ONLINE,   // Active in the last 2 minutes
    AWAY,     // Idle 2-10 minutes
    OFFLINE   // No sessions or idle > 10 minutes
}
