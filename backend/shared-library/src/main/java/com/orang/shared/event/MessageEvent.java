package com.orang.shared.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public abstract class MessageEvent {
    private UUID messageId;
    private UUID conversationId;
    private UUID triggeredBy;
    private LocalDateTime timestamp;
}