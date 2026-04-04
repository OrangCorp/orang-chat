package com.orang.messageservice.service;

import com.orang.messageservice.entity.Attachment;
import com.orang.messageservice.entity.FileType;
import com.orang.messageservice.repository.AttachmentRepository;
import com.orang.shared.exception.BadRequestException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class AttachmentService {

    private final AttachmentRepository attachmentRepository;
    private final FileStorageService fileStorageService;
    private final ConversationService conversationService;

    private static final long MAX_FILE_SIZE = 50 * 1024 * 1024; // 50 MB

    public Attachment uploadAttachment(
            MultipartFile file,
            UUID conversationId,
            UUID uploaderId) throws IOException {

        validateFile(file);

        conversationService.verifyParticipant(conversationId, uploaderId);

        UUID attachmentId = UUID.randomUUID();
        String storagePath = buildStoragePath(conversationId, attachmentId, file.getOriginalFilename());

        String storageKey = fileStorageService.uploadFile(
                file.getInputStream(),
                file.getOriginalFilename(),
                file.getContentType(),
                file.getSize(),
                storagePath
        );

        Attachment attachment = Attachment.builder()
                .id(attachmentId)
                .conversationId(conversationId)
                .uploaderId(uploaderId)
                .fileName(file.getOriginalFilename())
                .contentType(file.getContentType())
                .fileSize(file.getSize())
                .storageKey(storageKey)
                .build();

        Attachment saved = attachmentRepository.save(attachment);

        log.info("User {} uploaded attachment {} to conversation {}",
                uploaderId, saved.getId(), conversationId);

        return saved;
    }

    private String buildStoragePath(UUID conversationId, UUID attachmentId, String fileName) {
        String sanitized = sanitizeFilename(fileName);

        return String.format("%s/%s/%s", conversationId, attachmentId, sanitized);
    }

    private String sanitizeFilename(String fileName) {
        if (fileName == null) {
            return "file";
        }

        // Replace unsafe characters with underscore
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
