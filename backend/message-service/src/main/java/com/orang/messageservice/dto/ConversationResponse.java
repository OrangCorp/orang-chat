package com.orang.messageservice.dto;

import com.orang.messageservice.entity.Conversation.ConversationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConversationResponse {
    private UUID id;
    private ConversationType type;
    private String name;
    private Set<UUID> participantIds;
}
