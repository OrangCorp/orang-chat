package com.orang.messageservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PinnedMessagesResponse {
    private UUID conversationId;
    private List<UUID> pinnedMessageIds;
    private int count;
}