package com.orang.messageservice.service;

import com.orang.messageservice.entity.Attachment;
import com.orang.messageservice.entity.FileType;
import com.orang.messageservice.event.ThumbnailRequestedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ThumbnailServiceTest {

    @Mock
    private FileStorageService fileStorageService;

    @Mock
    private RabbitTemplate rabbitTemplate;

    private ThumbnailService thumbnailService;
    private UUID conversationId;
    private UUID attachmentId;

    @BeforeEach
    void setUp() {
        thumbnailService = new ThumbnailService(fileStorageService, rabbitTemplate);
        conversationId = UUID.randomUUID();
        attachmentId = UUID.randomUUID();
    }

    @Test
    void supportsFileTypeReturnsTrueForImage() {
        assertTrue(thumbnailService.supportsFileType(FileType.IMAGE));
    }

    @Test
    void supportsFileTypeReturnsFalseForDocument() {
        assertFalse(thumbnailService.supportsFileType(FileType.DOCUMENT));
    }

    @Test
    void supportsFileTypeReturnsFalseForVideo() {
        assertFalse(thumbnailService.supportsFileType(FileType.VIDEO));
    }

    @Test
    void supportsFileTypeReturnsFalseForAudio() {
        assertFalse(thumbnailService.supportsFileType(FileType.AUDIO));
    }

    @Test
    void requestThumbnailGenerationPublishesEventForImage() {
        Attachment attachment = Attachment.builder()
                .id(attachmentId)
                .conversationId(conversationId)
                .fileName("image.jpg")
                .fileSize(1024L)
                .contentType("image/jpeg")
                .storageKey("s3://bucket/image.jpg")
                .build();

        thumbnailService.requestThumbnailGeneration(attachment);

        verify(rabbitTemplate).convertAndSend(anyString(), anyString(), any(ThumbnailRequestedEvent.class));
    }

    @Test
    void requestThumbnailGenerationIgnoresDocumentFile() {
        Attachment attachment = Attachment.builder()
                .id(attachmentId)
                .conversationId(conversationId)
                .fileName("document.pdf")
                .fileSize(2048L)
                .contentType("application/pdf")
                .storageKey("s3://bucket/document.pdf")
                .build();

        thumbnailService.requestThumbnailGeneration(attachment);

        verify(rabbitTemplate, never()).convertAndSend(anyString(), anyString(), any(ThumbnailRequestedEvent.class));
    }

    @Test
    void requestThumbnailGenerationIgnoresVideoFile() {
        Attachment attachment = Attachment.builder()
                .id(attachmentId)
                .conversationId(conversationId)
                .fileName("video.mp4")
                .fileSize(10240L)
                .contentType("video/mp4")
                .storageKey("s3://bucket/video.mp4")
                .build();

        thumbnailService.requestThumbnailGeneration(attachment);

        verify(rabbitTemplate, never()).convertAndSend(anyString(), anyString(), any(ThumbnailRequestedEvent.class));
    }

    @Test
    void requestThumbnailGenerationSkipsUnknownContentType() {
        Attachment attachment = Attachment.builder()
                .id(attachmentId)
                .conversationId(conversationId)
                .fileName("unknown")
                .contentType("application/octet-stream")
                .build();

        thumbnailService.requestThumbnailGeneration(attachment);

        verify(rabbitTemplate, never()).convertAndSend(anyString(), anyString(), any(ThumbnailRequestedEvent.class));
    }
}
