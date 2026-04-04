package com.orang.shared.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GroupMemberEvent implements GroupEvent {
    private UUID conversationId;
    private UUID userId;
    private UUID triggeredBy;
    private EventType eventType;

    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();

    public enum EventType {
        MEMBER_ADDED,
        MEMBER_REMOVED,
        MEMBER_LEFT,
        ADMIN_PROMOTED,
        ADMIN_DEMOTED
    }
}