package com.orang.messageservice.dto;

import com.orang.messageservice.entity.Conversation;
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
public class ConversationResponse {
    private UUID id;
    private Conversation.ConversationType type;
    private String name;
    private UUID createdBy;
    private List<ParticipantInfo> participants;
    private LocalDateTime createdAt;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ParticipantInfo {
        private UUID userId;
        private String role;
        private LocalDateTime joinedAt;
        private UUID addedBy;
    }
}