package com.orang.messageservice.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "attachments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Attachment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "conversation_id", nullable = false)
    private UUID conversationId;

    @Column(name = "uploader_id", nullable = false)
    private UUID uploaderId;

    @Column(name = "message_id")
    private UUID messageId;

    @Column(name = "file_name", nullable = false)
    private String fileName;

    @Column(name = "content_type", nullable = false)
    private String contentType;

    @Column(name = "file_size", nullable = false)
    private Long fileSize;

    @Column(name = "storage_key", nullable = false, unique = true)
    private String storageKey;

    @Column(name = "thumbnail_storage_key")
    private String thumbnailStorageKey;

    @Column(name = "thumbnail_generated")
    @Builder.Default
    private Boolean thumbnailGenerated = false;

    @Column(name = "thumbnail_attempts")
    @Builder.Default
    private Integer thumbnailAttempts = 0;

    @Column(name = "thumbnail_last_attempt")
    private LocalDateTime thumbnailLastAttempt;

    @Column(name = "thumbnail_error")
    private String thumbnailError;

    @CreationTimestamp
    @Column(name = "uploaded_at", nullable = false, updatable = false)
    private LocalDateTime uploadedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Column(name = "permanently_deleted_at")
    private LocalDateTime permanentlyDeletedAt;

    public boolean isDeleted() {
        return deletedAt != null;
    }

    public boolean isExpired() {
        if (deletedAt == null) {
            return false;
        }
        return deletedAt.plusDays(30).isBefore(LocalDateTime.now());
    }

    public FileType getFileType() {
        return FileType.fromMimeType(contentType)
                .orElseThrow(() -> new IllegalArgumentException("Unsupported file type: " + contentType));
    }

    public void softDelete() {
        this.deletedAt = LocalDateTime.now();
    }

    public void linkToMessage(UUID messageId) {
        if (this.messageId != null) {
            throw new IllegalStateException("Already linked to message " + this.messageId);
        }
        this.messageId = messageId;
    }

    public boolean isOrphaned(int gracePeriodHours) {
        if (gracePeriodHours < 0) {
            throw new IllegalArgumentException("Grace period must be non-negative");
        }
        return messageId == null &&
                uploadedAt.plusHours(gracePeriodHours).isBefore(LocalDateTime.now());
    }

    public void recordThumbnailAttempt(String error) {
        this.thumbnailAttempts = (this.thumbnailAttempts == null ? 0 : this.thumbnailAttempts) + 1;
        this.thumbnailLastAttempt = LocalDateTime.now();
        this.thumbnailError = error;
    }

    public void markThumbnailSuccess(String storageKey) {
        this.thumbnailStorageKey = storageKey;
        this.thumbnailGenerated = true;
        this.thumbnailError = null;
    }

    public boolean canRetryThumbnail() {
        return !Boolean.TRUE.equals(this.thumbnailGenerated)
                && (this.thumbnailAttempts == null || this.thumbnailAttempts < 3);
    }
}
