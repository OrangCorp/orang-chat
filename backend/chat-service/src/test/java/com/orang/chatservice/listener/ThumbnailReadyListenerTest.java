package com.orang.chatservice.listener;

import com.orang.shared.event.ThumbnailReadyEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ThumbnailReadyListenerTest {

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @InjectMocks
    private ThumbnailReadyListener thumbnailReadyListener;

    private UUID attachmentId;
    private UUID messageId;
    private UUID conversationId;
    private String thumbnailUrl;

    @BeforeEach
    void setUp() {
        attachmentId = UUID.randomUUID();
        messageId = UUID.randomUUID();
        conversationId = UUID.randomUUID();
        thumbnailUrl = "https://bucket.s3.amazonaws.com/thumbnails/image.jpg";
    }

    @Test
    void handleThumbnailReadyBroadcastsEvent() {
        ThumbnailReadyEvent event = new ThumbnailReadyEvent();
        event.setAttachmentId(attachmentId);
        event.setMessageId(messageId);
        event.setConversationId(conversationId);
        event.setThumbnailUrl(thumbnailUrl);

        thumbnailReadyListener.handleThumbnailReady(event);

        ArgumentCaptor<Map<String, String>> payloadCaptor = ArgumentCaptor.forClass(Map.class);
        verify(messagingTemplate).convertAndSend(
                anyString(),
                payloadCaptor.capture()
        );

        Map<String, String> payload = payloadCaptor.getValue();
        assertEquals("THUMBNAIL_READY", payload.get("type"));
        assertEquals(attachmentId.toString(), payload.get("attachmentId"));
        assertEquals(messageId.toString(), payload.get("messageId"));
        assertEquals(thumbnailUrl, payload.get("thumbnailUrl"));
    }

    @Test
    void handleThumbnailReadyHandlesNullAttachmentId() {
        ThumbnailReadyEvent event = new ThumbnailReadyEvent();
        event.setAttachmentId(null);
        event.setMessageId(messageId);
        event.setConversationId(conversationId);
        event.setThumbnailUrl(thumbnailUrl);

        thumbnailReadyListener.handleThumbnailReady(event);

        ArgumentCaptor<Map<String, String>> payloadCaptor = ArgumentCaptor.forClass(Map.class);
        verify(messagingTemplate).convertAndSend(
                anyString(),
                payloadCaptor.capture()
        );

        Map<String, String> payload = payloadCaptor.getValue();
        assertNull(payload.get("attachmentId"));
    }

    @Test
    void handleThumbnailReadyHandlesNullMessageId() {
        ThumbnailReadyEvent event = new ThumbnailReadyEvent();
        event.setAttachmentId(attachmentId);
        event.setMessageId(null);
        event.setConversationId(conversationId);
        event.setThumbnailUrl(thumbnailUrl);

        thumbnailReadyListener.handleThumbnailReady(event);

        ArgumentCaptor<Map<String, String>> payloadCaptor = ArgumentCaptor.forClass(Map.class);
        verify(messagingTemplate).convertAndSend(
                anyString(),
                payloadCaptor.capture()
        );

        Map<String, String> payload = payloadCaptor.getValue();
        assertNull(payload.get("messageId"));
    }

    @Test
    void handleThumbnailReadyCatchesExceptionAndDoesNotRethrow() {
        ThumbnailReadyEvent event = new ThumbnailReadyEvent();
        event.setConversationId(conversationId);

        org.mockito.Mockito.doThrow(new RuntimeException("Messaging failed"))
                .when(messagingTemplate).convertAndSend(anyString(), anyString());

        // Should not throw
        thumbnailReadyListener.handleThumbnailReady(event);
    }
}
