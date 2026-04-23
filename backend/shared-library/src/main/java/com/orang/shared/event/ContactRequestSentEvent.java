package com.orang.shared.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.UUID;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class ContactRequestSentEvent {
    private UUID contactId;
    private UUID requesterId;
    private UUID recipientId;
}