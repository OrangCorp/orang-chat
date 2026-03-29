package com.orang.userservice.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "contacts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Contact {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "requester_id", nullable = false)
    private UUID requesterId;

    @Column(name = "recipient_id", nullable = false)
    private UUID recipientId;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private ContactStatus status = ContactStatus.PENDING;

    private LocalDateTime acceptedAt;

    private UUID blockedBy;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    public boolean isRequester(UUID userId) {
        return requesterId.equals(userId);
    }

    public boolean isRecipient(UUID userId) {
        return recipientId.equals(userId);
    }

    public boolean involvesUser(UUID userId) {
        return isRequester(userId) || isRecipient(userId);
    }

    public boolean isBlocked() {
        return blockedBy != null;
    }

    public UUID getOtherUser(UUID userId) {
        if (isRequester(userId)) {
            return recipientId;
        } else if (isRecipient(userId)) {
            return requesterId;
        }
        throw new IllegalArgumentException("User " + userId + " is not part of this contact");
    }
}
