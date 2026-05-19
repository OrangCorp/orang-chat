package com.orang.messageservice.service;

import com.orang.messageservice.entity.Attachment;
import com.orang.messageservice.entity.FileType;
import com.orang.messageservice.repository.AttachmentRepository;
import com.orang.shared.exception.BadRequestException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AttachmentServiceTest {

    @Mock
    private AttachmentRepository attachmentRepository;

    @Mock
    private FileStorageService fileStorageService;

    @Mock
    private ConversationService conversationService;

    @Mock
    private ThumbnailService thumbnailService;

    @Mock
    private MultipartFile multipartFile;

    private AttachmentService attachmentService;
    private UUID conversationId;
    private UUID uploaderId;

    @BeforeEach
    void setUp() {
        attachmentService = new AttachmentService(
                attachmentRepository, fileStorageService, conversationService, thumbnailService);
        // Set max file size to 50MB via reflection
        ReflectionTestUtils.setField(attachmentService, "downloadMode", "backend");

        conversationId = UUID.randomUUID();
        uploaderId = UUID.randomUUID();
    }

    // ============ uploadAttachment Tests ============

    @Test
    void uploadAttachmentThrowsWhenUserNotParticipant() throws IOException {
        lenient().doThrow(new RuntimeException("Not a participant"))
                .when(conversationService).verifyParticipant(conversationId, uploaderId);

        assertThrows(RuntimeException.class,
                () -> attachmentService.uploadAttachment(multipartFile, conversationId, uploaderId));

        verifyNoInteractions(fileStorageService);
    }

    @Test
    void uploadAttachmentThrowsWhenFileNull() throws IOException {
        lenient().when(multipartFile.getOriginalFilename()).thenReturn(null);

        assertThrows(BadRequestException.class,
                () -> attachmentService.uploadAttachment(multipartFile, conversationId, uploaderId));

        verifyNoInteractions(fileStorageService);
    }

    @Test
    void uploadAttachmentThrowsWhenFileEmpty() throws IOException {
        lenient().when(multipartFile.isEmpty()).thenReturn(true);

        assertThrows(BadRequestException.class,
                () -> attachmentService.uploadAttachment(multipartFile, conversationId, uploaderId));

        verifyNoInteractions(fileStorageService);
    }

    @Test
    void uploadAttachmentThrowsWhenFileTooLarge() throws IOException {
        lenient().when(multipartFile.isEmpty()).thenReturn(false);
        lenient().when(multipartFile.getOriginalFilename()).thenReturn("huge.bin");
        lenient().when(multipartFile.getSize()).thenReturn(75 * 1024 * 1024L); // 75 MB > 50 MB limit

        assertThrows(BadRequestException.class,
                () -> attachmentService.uploadAttachment(multipartFile, conversationId, uploaderId));

        verifyNoInteractions(fileStorageService);
    }

    @Test
    void uploadAttachmentSuccessfullyUploadsValidFile() throws IOException {
        doNothing().when(conversationService).verifyParticipant(conversationId, uploaderId);

        Attachment saved = Attachment.builder()
                .id(UUID.randomUUID())
                .conversationId(conversationId)
                .uploaderId(uploaderId)
                .fileName("test.txt")
                .contentType("text/plain")
                .fileSize(100L)
                .storageKey("s3://bucket/file")
                .build();

        when(multipartFile.getOriginalFilename()).thenReturn("test.txt");
        when(multipartFile.getContentType()).thenReturn("text/plain");
        when(multipartFile.getSize()).thenReturn(100L);
        when(multipartFile.getInputStream()).thenReturn(new ByteArrayInputStream("content".getBytes()));
        when(attachmentRepository.save(any(Attachment.class)))
                .thenReturn(saved);
        when(fileStorageService.uploadFile(any(), any(), any(), anyLong(), any()))
                .thenReturn("s3://bucket/file");

        Attachment result = attachmentService.uploadAttachment(multipartFile, conversationId, uploaderId);

        assertNotNull(result);
        assertEquals("test.txt", result.getFileName());
        assertEquals("text/plain", result.getContentType());
        assertEquals(100L, result.getFileSize());

        verify(fileStorageService).uploadFile(any(), any(), any(), anyLong(), any());
        verify(attachmentRepository, times(2)).save(any()); // Save twice: initial and with storage key
    }

    // ============ getDownloadUrl Tests ============

    @Test
    void getDownloadUrlReturnsPresignedUrl() {
        // Set download mode to minio to enable presigned URLs
        ReflectionTestUtils.setField(attachmentService, "downloadMode", "minio");

        UUID attachmentId = UUID.randomUUID();
        Attachment attachment = Attachment.builder()
                .id(attachmentId)
                .storageKey("s3://bucket/file")
                .build();

        when(attachmentRepository.findById(attachmentId))
                .thenReturn(java.util.Optional.of(attachment));
        when(fileStorageService.generatePresignedDownloadUrl(any(), any()))
                .thenReturn("https://presigned-url");

        String url = attachmentService.getDownloadUrl(attachmentId);

        assertEquals("https://presigned-url", url);
        verify(fileStorageService).generatePresignedDownloadUrl("s3://bucket/file", java.time.Duration.ofHours(1));
    }

    @Test
    void getDownloadUrlThrowsWhenAttachmentNotFound() {
        UUID attachmentId = UUID.randomUUID();

        when(attachmentRepository.findById(attachmentId))
                .thenReturn(java.util.Optional.empty());

        assertThrows(Exception.class,
                () -> attachmentService.getDownloadUrl(attachmentId));

        verifyNoInteractions(fileStorageService);
    }

    // ============ softDeleteAttachment Tests ============

    @Test
    void softDeleteAttachmentRemovesLogicallyFromDatabase() {
        UUID attachmentId = UUID.randomUUID();
        Attachment attachment = Attachment.builder()
                .id(attachmentId)
                .storageKey("s3://bucket/file")
                .conversationId(conversationId)
                .uploaderId(uploaderId)
                .build();

        when(attachmentRepository.findById(attachmentId))
                .thenReturn(java.util.Optional.of(attachment));

        attachmentService.softDeleteAttachment(attachmentId, uploaderId);

        // Soft delete marks deletedAt timestamp but doesn't remove from storage
        verify(attachmentRepository).save(argThat(att ->
                att.getId().equals(attachmentId) && att.isDeleted()));
    }

    @Test
    void softDeleteAttachmentThrowsWhenNotUploaderAndNotAdmin() {
        UUID attachmentId = UUID.randomUUID();
        UUID otherUserId = UUID.randomUUID();

        Attachment attachment = Attachment.builder()
                .id(attachmentId)
                .uploaderId(uploaderId)
                .conversationId(conversationId)
                .build();

        when(attachmentRepository.findById(attachmentId))
                .thenReturn(java.util.Optional.of(attachment));
        assertThrows(Exception.class,
                () -> attachmentService.softDeleteAttachment(attachmentId, otherUserId));

        verify(attachmentRepository, never()).save(any());
    }
}
