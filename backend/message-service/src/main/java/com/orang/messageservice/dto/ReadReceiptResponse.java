package com.orang.messageservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReadReceiptResponse {
    private UUID conversationId;
    private UUID lastReadMessageId;
    private long unreadCount;
}