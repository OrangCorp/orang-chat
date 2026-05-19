package com.orang.messageservice.scheduler;

import com.orang.messageservice.entity.Attachment;
import com.orang.messageservice.repository.AttachmentRepository;
import com.orang.messageservice.service.ThumbnailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ThumbnailRetryJobTest {

    @Mock
    private AttachmentRepository attachmentRepository;

    @Mock
    private ThumbnailService thumbnailService;

    @InjectMocks
    private ThumbnailRetryJob thumbnailRetryJob;

    private UUID conversationId;

    @BeforeEach
    void setUp() {
        conversationId = UUID.randomUUID();
    }

    @Test
    void retryFailedThumbnailsMarksSuccessNullAndFailurePaths() throws Exception {
        Attachment success = buildAttachment("original-1");
        Attachment returnedNull = buildAttachment("original-2");
        Attachment failure = buildAttachment("original-3");

        when(attachmentRepository.findAttachmentsNeedingThumbnailRetry(any())).thenReturn(List.of(success, returnedNull, failure));
        when(thumbnailService.generateAndUploadThumbnail(eq("original-1"), eq(conversationId), eq(success.getId())))
                .thenReturn("thumb-1");
        when(thumbnailService.generateAndUploadThumbnail(eq("original-2"), eq(conversationId), eq(returnedNull.getId())))
                .thenReturn(null);
        when(thumbnailService.generateAndUploadThumbnail(eq("original-3"), eq(conversationId), eq(failure.getId())))
                .thenThrow(new RuntimeException("boom"));
        when(attachmentRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        thumbnailRetryJob.retryFailedThumbnails();

        assertThat(success.getThumbnailGenerated()).isTrue();
        assertThat(success.getThumbnailStorageKey()).isEqualTo("thumb-1");
        assertThat(returnedNull.getThumbnailGenerated()).isFalse();
        assertThat(returnedNull.getThumbnailError()).isEqualTo("Retry returned null");
        assertThat(failure.getThumbnailGenerated()).isFalse();
        assertThat(failure.getThumbnailError()).contains("boom");
        verify(attachmentRepository, times(3)).save(any());
    }

    private Attachment buildAttachment(String storageKey) {
        return Attachment.builder()
                .id(UUID.randomUUID())
                .conversationId(conversationId)
                .uploaderId(UUID.randomUUID())
                .fileName(storageKey + ".jpg")
                .contentType("image/jpeg")
                .fileSize(100L)
                .storageKey(storageKey)
                .build();
    }
}
