package com.orang.messageservice.event;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@AllArgsConstructor
public class MessageEditedInternalEvent {
    private final UUID messageId;
    private final UUID conversationId;
    private final UUID userId;
    private final String newContent;
    private final LocalDateTime editedAt;
}
