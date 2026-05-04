package com.orang.messageservice.controller;

import com.orang.messageservice.entity.Attachment;
import com.orang.messageservice.service.AttachmentService;
import com.orang.messageservice.service.FileStorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AttachmentControllerTest {

    @Mock
    private AttachmentService attachmentService;

    @Mock
    private FileStorageService fileStorageService;

    private AttachmentController attachmentController;

    @BeforeEach
    void setUp() {
        attachmentController = new AttachmentController(attachmentService, fileStorageService);
        ReflectionTestUtils.setField(attachmentController, "downloadMode", "backend");
    }

    @Test
    void downloadThumbnailReturnsTooEarlyWhenThumbnailIsNotReady() throws Exception {
        UUID attachmentId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        Attachment attachment = Attachment.builder()
                .id(attachmentId)
                .thumbnailGenerated(false)
                .thumbnailStorageKey(null)
                .build();

        when(attachmentService.getAttachment(attachmentId, userId)).thenReturn(attachment);

        var response = attachmentController.downloadThumbnail(attachmentId, userId.toString());

        assertEquals(HttpStatus.TOO_EARLY, response.getStatusCode());
        Object body = assertInstanceOf(Object.class, response.getBody());
        assertEquals("THUMBNAIL_NOT_READY", ReflectionTestUtils.getField(body, "code"));
        assertEquals("Thumbnail is still being generated", ReflectionTestUtils.getField(body, "message"));
    }
}