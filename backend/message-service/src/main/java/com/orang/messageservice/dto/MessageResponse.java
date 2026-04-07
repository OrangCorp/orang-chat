package com.orang.messageservice.dto;

import com.orang.messageservice.entity.FileType;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class MessageResponse {

    private UUID id;
    private UUID conversationId;
    private UUID senderId;
    private String content;
    private LocalDateTime createdAt;
    private boolean edited;
    private LocalDateTime editedAt;
    private boolean deleted;
    private LocalDateTime deletedAt;

    private List<AttachmentInfo> attachments;

    @Data
    @Builder
    public static class AttachmentInfo {
        private UUID id;
        private String fileName;
        private String contentType;
        private Long fileSize;
        private FileType fileType;
        private String downloadUrl;
    }
}