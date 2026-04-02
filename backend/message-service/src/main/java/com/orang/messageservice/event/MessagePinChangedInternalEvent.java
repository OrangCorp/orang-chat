package com.orang.messageservice.event;

import com.orang.shared.event.MessagePinnedEvent;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.UUID;

@Getter
@AllArgsConstructor
public class MessagePinChangedInternalEvent {
    private final UUID messageId;
    private final UUID conversationId;
    private final UUID userId;
    private final MessagePinnedEvent.Action action;
}
