package com.orang.messageservice.event;

import com.orang.messageservice.entity.ReactionType;
import com.orang.shared.event.MessageReactionEvent;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Map;
import java.util.UUID;

@Getter
@AllArgsConstructor
public class MessageReactionChangedInternalEvent {
    private final UUID messageId;
    private final UUID conversationId;
    private final UUID userId;
    private final MessageReactionEvent.Action action;
    private final ReactionType reactionType;
    private final Map<ReactionType, Long> currentCounts;
}
