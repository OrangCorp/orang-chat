package com.orang.chatservice.dto;

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
public class ChatMessage {
    private UUID senderId;
    private UUID recipientId;
    private String content;
    private LocalDateTime timestamp;
    private MessageType type;
}
