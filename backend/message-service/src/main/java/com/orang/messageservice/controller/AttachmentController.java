package com.orang.messageservice.controller;

import com.orang.messageservice.dto.AttachmentResponse;
import com.orang.messageservice.dto.DownloadUrlResponse;
import com.orang.messageservice.entity.Attachment;
import com.orang.messageservice.service.AttachmentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

@RestController
@RequestMapping("/api/attachments")
@RequiredArgsConstructor
@Slf4j
public class AttachmentController {

    private final AttachmentService attachmentService;

    @Value("${minio.download-mode:backend}")
    private String downloadMode;

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<AttachmentResponse> uploadAttachment(
            @RequestParam("file") MultipartFile file,
            @RequestParam("conversationId") UUID conversationId,
            @AuthenticationPrincipal String myUserId) throws IOException {

        UUID userUUID = UUID.fromString(myUserId);

        log.info("Upload request: file={}, size={}, conversation={}, user={}",
                file.getOriginalFilename(),
                file.getSize(),
                conversationId,
                userUUID);

        Attachment attachment = attachmentService.uploadAttachment(file, conversationId, userUUID);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(toResponse(attachment));
    }

    @GetMapping("/{attachmentId}")
    public ResponseEntity<AttachmentResponse> getAttachment(
            @PathVariable UUID attachmentId,
            @AuthenticationPrincipal String userIdStr) {

        UUID userId = UUID.fromString(userIdStr);
        Attachment attachment = attachmentService.getAttachment(attachmentId, userId);

        return ResponseEntity.ok(toResponse(attachment));
    }

    @GetMapping("/{attachmentId}/download")
    public ResponseEntity<?> downloadAttachment(
            @PathVariable UUID attachmentId,
            @AuthenticationPrincipal String userId) throws IOException {

        UUID userUUID = UUID.fromString(userId);

        // This will return either a presigned URL or a backend download path based on mode
        String url = attachmentService.getDownloadUrl(attachmentId, userUUID);

        if ("minio".equalsIgnoreCase(downloadMode)) {
            return ResponseEntity.ok(new DownloadUrlResponse(url, 3600));
        }

        // Mode 2: Stream through backend (backend downloads from MinIO, streams to client)
        var attachment = attachmentService.getAttachment(attachmentId, userUUID);
        var inputStream = attachmentService.downloadAttachment(attachmentId, userUUID);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(attachment.getContentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + attachment.getFileName() + "\"")
                .body(new InputStreamResource(inputStream));
    }

    @DeleteMapping("/{attachmentId}")
    public ResponseEntity<Void> deleteAttachment(
            @PathVariable UUID attachmentId,
            @AuthenticationPrincipal String userIdStr) {

        UUID userId = UUID.fromString(userIdStr);
        attachmentService.softDeleteAttachment(attachmentId, userId);

        return ResponseEntity.noContent().build();
    }

    private AttachmentResponse toResponse(Attachment attachment) {
        String thumbnailUrl = null;
        if (Boolean.TRUE.equals(attachment.getThumbnailGenerated())) {
            thumbnailUrl = "/api/attachments/" + attachment.getId() + "/thumbnail";
        }

        return AttachmentResponse.builder()
                .id(attachment.getId())
                .conversationId(attachment.getConversationId())
                .uploaderId(attachment.getUploaderId())
                .messageId(attachment.getMessageId())
                .fileName(attachment.getFileName())
                .contentType(attachment.getContentType())
                .fileSize(attachment.getFileSize())
                .fileType(attachment.getFileType().name())
                .uploadedAt(attachment.getUploadedAt())
                .thumbnailAvailable(Boolean.TRUE.equals(attachment.getThumbnailGenerated()))
                .thumbnailUrl(thumbnailUrl)
                .build();
    }
}
