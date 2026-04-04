package com.orang.shared.event;

import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class MessageDeletedEvent extends MessageEvent {
    private UUID deletedBy;
    private LocalDateTime deletedAt;
}