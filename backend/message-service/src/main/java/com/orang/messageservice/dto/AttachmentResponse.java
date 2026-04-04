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
public class AttachmentResponse {
    private UUID id;
    private UUID conversationId;
    private UUID uploaderId;
    private UUID messageId;
    private String fileName;
    private String contentType;
    private Long fileSize;
    private String fileType;
    private LocalDateTime uploadedAt;
}