package com.orang.messageservice.service;

import com.orang.messageservice.entity.Attachment;
import com.orang.messageservice.entity.FileType;
import com.orang.messageservice.repository.AttachmentRepository;
import com.orang.shared.exception.BadRequestException;
import com.orang.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class AttachmentService {

    private final AttachmentRepository attachmentRepository;
    private final FileStorageService fileStorageService;
    private final ConversationService conversationService;
    private final ThumbnailService thumbnailService;

    @Value("${minio.download-mode:backend}")
    private String downloadMode;

    private static final long MAX_FILE_SIZE = 50 * 1024 * 1024; // 50 MB
    private static final Duration DOWNLOAD_URL_EXPIRY = Duration.ofHours(1);

    public Attachment uploadAttachment(
            MultipartFile file,
            UUID conversationId,
            UUID uploaderId) throws IOException {

        validateFile(file);
        conversationService.verifyParticipant(conversationId, uploaderId);

        String originalFilename = file.getOriginalFilename();
        String contentType = file.getContentType();
        long fileSize = file.getSize();

        Attachment attachment = Attachment.builder()
                .conversationId(conversationId)
                .uploaderId(uploaderId)
                .fileName(originalFilename)
                .contentType(contentType)
                .fileSize(fileSize)
                .storageKey("temp") // Temporary placeholder
                .build();

        Attachment saved = attachmentRepository.save(attachment);

        String storagePath = buildStoragePath(conversationId, saved.getId(), originalFilename);

        String storageKey = fileStorageService.uploadFile(
                file.getInputStream(),
                originalFilename,
                contentType,
                fileSize,
                storagePath
        );

        saved.setStorageKey(storageKey);
        saved = attachmentRepository.save(saved);

        log.info("User {} uploaded attachment {} to conversation {}",
                uploaderId, saved.getId(), conversationId);

        thumbnailService.requestThumbnailGeneration(saved);
        return saved;
    }

    @Transactional(readOnly = true)
    public String getDownloadUrl(UUID attachmentId) {
        Attachment attachment = findAttachmentOrThrow(attachmentId);

        if (attachment.isDeleted()) {
            throw new ResourceNotFoundException("Attachment has been deleted");
        }

        if ("minio".equalsIgnoreCase(downloadMode)) {
            return fileStorageService.generatePresignedDownloadUrl(
                    attachment.getStorageKey(),
                    DOWNLOAD_URL_EXPIRY
            );
        }

        return "/api/attachments/" + attachmentId + "/download";
    }

    @Transactional(readOnly = true)
    public String getDownloadUrl(UUID attachmentId, UUID userId) {
        Attachment attachment = findAttachmentOrThrow(attachmentId);

        conversationService.verifyParticipant(attachment.getConversationId(), userId);

        return getDownloadUrl(attachmentId);
    }

    @Transactional(readOnly = true)
    public InputStream downloadAttachment(UUID attachmentId, UUID userId) throws IOException {
        Attachment attachment = findAttachmentOrThrow(attachmentId);

        conversationService.verifyParticipant(attachment.getConversationId(), userId);

        if (attachment.isDeleted()) {
            throw new ResourceNotFoundException("Attachment has been deleted");
        }

        return fileStorageService.downloadFile(attachment.getStorageKey());
    }

    @Transactional(readOnly = true)
    public Attachment getAttachment(UUID attachmentId, UUID userId) {
        Attachment attachment = findAttachmentOrThrow(attachmentId);

        conversationService.verifyParticipant(attachment.getConversationId(), userId);

        if (attachment.isDeleted()) {
            throw new ResourceNotFoundException("Attachment has been deleted");
        }

        return attachment;
    }

    public void softDeleteAttachment(UUID attachmentId, UUID userId) {
        Attachment attachment = findAttachmentOrThrow(attachmentId);

        if (!attachment.getUploaderId().equals(userId)) {
            throw new BadRequestException("You can only delete your own attachments");
        }

        if (attachment.isDeleted()) {
            throw new BadRequestException("Attachment is already deleted");
        }

        attachment.softDelete();
        attachmentRepository.save(attachment);

        log.info("User {} soft-deleted attachment {}", userId, attachmentId);
    }

    @Transactional
    public void linkAttachmentsToMessage(List<UUID> attachmentIds, UUID messageId, UUID userId) {
        if (attachmentIds == null || attachmentIds.isEmpty()) {
            return;
        }

        if (attachmentIds.size() > 5) {
            throw new BadRequestException("Maximum 5 attachments per message");
        }

        int updated = attachmentRepository.linkAttachmentsToMessage(attachmentIds, messageId, userId);

        if (updated != attachmentIds.size()) {
            log.warn("Failed to link some attachments. Requested: {}, Updated: {}. " +
                    "This may be due to: some attachments not found, not belonging to user, already linked, or deleted.",
                    attachmentIds.size(), updated);
            throw new BadRequestException("Some attachments could not be linked (not found, unauthorized, or already linked)");
        }

        log.info("Linked {} attachments to message {}", updated, messageId);
    }

    @Transactional(readOnly = true)
    public List<Attachment> getAttachmentsForMessage(UUID messageId) {
        return attachmentRepository.findByMessageIdAndDeletedAtIsNull(messageId);
    }

    private Attachment findAttachmentOrThrow(UUID attachmentId) {
        return attachmentRepository.findById(attachmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Attachment not found"));
    }

    private String buildStoragePath(UUID conversationId, UUID attachmentId, String fileName) {
        String sanitized = sanitizeFilename(fileName);
        return String.format("%s/%s/%s", conversationId, attachmentId, sanitized);
    }

    private String sanitizeFilename(String fileName) {
        if (fileName == null) {
            return "file";
        }
        return fileName.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    private void validateFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new BadRequestException("File is empty");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BadRequestException(
                    String.format("File size exceeds limit. Max: 50 MB, Actual: %.2f MB",
                            file.getSize() / (1024.0 * 1024.0))
            );
        }

        String contentType = file.getContentType();
        if (contentType == null || !FileType.isSupported(contentType)) {
            throw new BadRequestException("Unsupported file type: " + contentType);
        }
    }
}