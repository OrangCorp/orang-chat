package com.orang.messageservice.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatMessageEvent {
    private UUID senderId;
    private UUID recipientId;
    private String content;
    private String type;

    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();
}
