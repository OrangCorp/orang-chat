package com.orang.messageservice.event;

import com.orang.shared.event.GroupMemberEvent;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.UUID;

@Getter
@AllArgsConstructor
public class GroupMemberInternalEvent {
    private final UUID conversationId;
    private final UUID userId;
    private final UUID triggeredBy;
    private final GroupMemberEvent.EventType eventType;
}
