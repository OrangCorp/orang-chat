package com.orang.messageservice.scheduler;

import com.orang.messageservice.entity.Attachment;
import com.orang.messageservice.repository.AttachmentRepository;
import com.orang.messageservice.service.FileStorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AttachmentCleanupJobTest {

    @Mock
    private AttachmentRepository attachmentRepository;

    @Mock
    private FileStorageService fileStorageService;

    @InjectMocks
    private AttachmentCleanupJob attachmentCleanupJob;

    private UUID conversationId;
    private UUID uploaderId;

    @BeforeEach
    void setUp() {
        conversationId = UUID.randomUUID();
        uploaderId = UUID.randomUUID();
    }

    @Test
    void cleanupExpiredAttachmentsDeletesStoredFilesAndMarksPermanentDeletion() {
        Attachment expired = Attachment.builder()
                .id(UUID.randomUUID())
                .conversationId(conversationId)
                .uploaderId(uploaderId)
                .fileName("photo.jpg")
                .contentType("image/jpeg")
                .fileSize(100L)
                .storageKey("attachments/original")
                .thumbnailStorageKey("attachments/thumb")
                .deletedAt(LocalDateTime.now().minusDays(31))
                .build();

        when(attachmentRepository.findExpiredAttachments(any())).thenReturn(List.of(expired));
        doNothing().when(fileStorageService).deleteFile(anyString());
        when(attachmentRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        attachmentCleanupJob.cleanupExpiredAttachments();

        assertThat(expired.getPermanentlyDeletedAt()).isNotNull();
        verify(fileStorageService).deleteFile("attachments/original");
        verify(fileStorageService).deleteFile("attachments/thumb");
        verify(attachmentRepository).save(expired);
    }

    @Test
    void cleanupOrphanedAttachmentsSkipsRecentlyUploadedAttachment() {
        Attachment orphan = Attachment.builder()
                .id(UUID.randomUUID())
                .conversationId(conversationId)
                .uploaderId(uploaderId)
                .fileName("orphan.jpg")
                .contentType("image/jpeg")
                .fileSize(100L)
                .storageKey("attachments/orphan")
                .uploadedAt(LocalDateTime.now().minusHours(25))
                .build();

        Attachment notYetOrphaned = Attachment.builder()
                .id(UUID.randomUUID())
                .conversationId(conversationId)
                .uploaderId(uploaderId)
                .fileName("fresh.jpg")
                .contentType("image/jpeg")
                .fileSize(100L)
                .storageKey("attachments/fresh")
                .uploadedAt(LocalDateTime.now())
                .build();

        when(attachmentRepository.findOrphanedAttachments(any())).thenReturn(List.of(orphan, notYetOrphaned));
        doNothing().when(fileStorageService).deleteFile(anyString());

        attachmentCleanupJob.cleanupOrphanedAttachments();

        verify(fileStorageService).deleteFile("attachments/orphan");
        verify(fileStorageService, never()).deleteFile("attachments/fresh");
        verify(attachmentRepository).delete(orphan);
        verify(attachmentRepository, never()).delete(notYetOrphaned);
    }
}
