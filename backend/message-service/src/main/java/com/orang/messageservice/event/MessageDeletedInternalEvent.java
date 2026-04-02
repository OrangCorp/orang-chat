package com.orang.messageservice.event;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@AllArgsConstructor
public class MessageDeletedInternalEvent {
    private final UUID messageId;
    private final UUID conversationId;
    private final UUID deletedBy;
    private final LocalDateTime deletedAt;
}
