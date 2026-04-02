package com.orang.shared.event;

import lombok.*;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class MessagePinnedEvent extends MessageEvent {

    public enum Action {
        PINNED, UNPINNED
    }

    private Action action;
}