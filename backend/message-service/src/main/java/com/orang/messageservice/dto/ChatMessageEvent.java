package com.orang.messageservice.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class ChatMessageEvent {
    private UUID senderId;
    private UUID recipientId;
    private String content;
    private LocalDateTime timestamp;
    private String type;
}
