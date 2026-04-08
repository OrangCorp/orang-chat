package com.chatapp.notificationservice.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "notification_preferences")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationPreferences {

    @EmbeddedId
    private NotificationPreferencesId id;

    @Column(name = "muted", nullable = false)
    private boolean muted;

    @Column(name = "muted_until")
    private LocalDateTime mutedUntil;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public NotificationPreferences(UUID userId, UUID conversationId) {
        this.id = new NotificationPreferencesId(userId, conversationId);
        this.muted = false;
    }
}
