package com.orang.messageservice.dto;

import com.orang.messageservice.entity.FileType;
import com.orang.messageservice.entity.ReactionType;
import java.util.Map;
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

    private ReplyPreview replyTo;
    private List<UUID> mentionedUserIds;
    private Map<ReactionType, Long> reactionCounts;
    private List<ReactionInfo> reactions;

    @Data
    @Builder
    public static class AttachmentInfo {
        private UUID id;
        private String fileName;
        private String contentType;
        private Long fileSize;
        private FileType fileType;
        private String downloadUrl;

        private boolean thumbnailAvailable;
        private String thumbnailUrl;
    }

    @Data
    @Builder
    public static class ReplyPreview {
        private UUID messageId;
        private UUID senderId;
        private String contentPreview;
        private boolean deleted;
    }

    @Data
    @Builder
    public static class ReactionInfo {
        private UUID id;
        private UUID userId;
        private ReactionType reactionType;
        private java.time.LocalDateTime createdAt;
    }
}