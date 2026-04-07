package com.orang.shared.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessagePayload {
    private UUID senderId;
    private UUID recipientId;
    private UUID conversationId;
    private String content;
    private MessageType type;
    private List<UUID> attachmentIds;

    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();
}