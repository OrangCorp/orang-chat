package com.orang.messageservice.controller;

import com.orang.messageservice.dto.AttachmentResponse;
import com.orang.messageservice.dto.DownloadUrlResponse;
import com.orang.messageservice.entity.Attachment;
import com.orang.messageservice.service.AttachmentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
    public ResponseEntity<DownloadUrlResponse> getDownloadUrl(
            @PathVariable UUID attachmentId,
            @AuthenticationPrincipal String userIdStr) {

        UUID userId = UUID.fromString(userIdStr);
        String url = attachmentService.getDownloadUrl(attachmentId, userId);

        return ResponseEntity.ok(new DownloadUrlResponse(url, 3600));
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
                .build();
    }
}
