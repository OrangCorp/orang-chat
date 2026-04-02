package com.orang.messageservice.entity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "read_receipts")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReadReceipt {

    @EmbeddedId
    private ReadReceiptId id;

    @Column(name = "last_read_message_id", nullable = false)
    private UUID lastReadMessageId;

    @Column(name = "read_at", nullable = false)
    private LocalDateTime readAt;


    public UUID getUserId() {
        return id != null ? id.getUserId() : null;
    }

    public UUID getConversationId() {
        return id != null ? id.getConversationId() : null;
    }

    // Update the pointer to the last read message
    public void updatePointer(UUID messageId) {
        this.lastReadMessageId = messageId;
        this.readAt = LocalDateTime.now();
    }
}
