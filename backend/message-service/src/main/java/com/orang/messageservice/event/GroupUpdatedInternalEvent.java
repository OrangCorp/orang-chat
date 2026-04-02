package com.orang.messageservice.event;

import com.orang.shared.event.GroupUpdatedEvent;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.UUID;

@Getter
@AllArgsConstructor
public class GroupUpdatedInternalEvent {
    private final UUID conversationId;
    private final UUID triggeredBy;
    private final GroupUpdatedEvent.UpdateType updateType;
    private final String newName;
}
