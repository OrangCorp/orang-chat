package com.orang.notificationservice.entity;

import lombok.Getter;

@Getter
public enum NotificationType {
    NEW_MESSAGE("New Message"),
    REACTION("Reaction"),
    MENTION("Mention"),
    CONTACT_REQUEST("Contact Request"),
    CONTACT_ACCEPTED("Contact Accepted"),
    DIRECT_CONVERSATION_CREATED("New Chat"),
    GROUP_ADDED("Added to Group"),
    GROUP_REMOVED("Removed from Group"),
    ADMIN_PROMOTED("Promoted to Admin"),
    ADMIN_DEMOTED("Demoted from Admin"),
    GROUP_UPDATED("Group Updated");

    private final String displayName;

    NotificationType(String displayName) {
        this.displayName = displayName;
    }
}