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
public class DirectConversationCreatedEvent {
    private UUID conversationId;
    private UUID initiatorId;
    private UUID recipientId;

    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();
}
