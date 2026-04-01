package com.orang.shared.event;

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
public class GroupUpdatedEvent implements GroupEvent {
    private UUID conversationId;
    private UUID triggeredBy;
    private UpdateType updateType;
    private String newName;

    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();

    public enum UpdateType {
        RENAMED,
        DELETED
    }
}