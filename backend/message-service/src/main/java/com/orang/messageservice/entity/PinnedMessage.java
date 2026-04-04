package com.orang.messageservice.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "pinned_messages")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PinnedMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "conversation_id", nullable = false)
    private UUID conversationId;

    @Column(name = "message_id", nullable = false)
    private UUID messageId;

    @Column(name = "pinned_by", nullable = false)
    private UUID pinnedBy;

    @CreationTimestamp
    @Column(name = "pinned_at", nullable = false, updatable = false)
    private LocalDateTime pinnedAt;
}