package com.orang.messageservice.dto;

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
public class MessageSearchResponse {

    private UUID id;
    private UUID conversationId;
    private UUID senderId;
    private String content;
    private String highlightedContent;
    private Float rank;
    private LocalDateTime createdAt;
}
