package com.orang.shared.event;

import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.Map;
import java.util.UUID;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class MessageReactionEvent extends MessageEvent {

    public enum Action {
        ADDED, REMOVED, CHANGED
    }

    private Action action;
    private String reactionType;  // "LIKE", "HEART", "ORANG", etc.
    private Map<String, Long> currentCounts;  // {"LIKE": 5, "ORANG": 2}
    private UUID messageAuthorId;
}