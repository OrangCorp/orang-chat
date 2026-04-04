package com.orang.shared.event;

import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class MessageEditedEvent extends MessageEvent {
    private String newContent;
    private LocalDateTime editedAt;
}