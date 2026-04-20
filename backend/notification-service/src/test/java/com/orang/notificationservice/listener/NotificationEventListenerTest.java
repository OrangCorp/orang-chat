package com.orang.notificationservice.listener;

import com.orang.notificationservice.dto.NotificationPayload;
import com.orang.notificationservice.service.WebPushService;
import com.orang.shared.event.ContactRequestSentEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class NotificationEventListenerTest {

    @Mock
    private WebPushService webPushService;

    private NotificationEventListener listener;

    @BeforeEach
    void setUp() {
        listener = new NotificationEventListener(webPushService);
    }

    @Test
    void handleContactRequestSentSendsNotificationToRecipient() {
        UUID contactId = UUID.fromString("2f3c4f2d-2eb3-49f9-9db8-0f5b5ff1a111");
        UUID requesterId = UUID.fromString("844ec9f6-f781-4f67-aab0-1f33cf9734f7");
        UUID recipientId = UUID.fromString("bc9e3f0b-8c33-4ef0-8c8d-b1d0f3f8f9d2");

        listener.handleContactRequestSent(ContactRequestSentEvent.builder()
                .contactId(contactId)
                .requesterId(requesterId)
                .recipientId(recipientId)
                .build());

        ArgumentCaptor<NotificationPayload> payloadCaptor = ArgumentCaptor.forClass(NotificationPayload.class);
        verify(webPushService).sendToUser(eq(recipientId), payloadCaptor.capture());

        NotificationPayload payload = payloadCaptor.getValue();
        assertThat(payload.getTitle()).isEqualTo("New Contact Request");
        assertThat(payload.getBody()).isEqualTo("You have a new contact request");
        assertThat(payload.getTag()).isEqualTo("contact-request-" + contactId);
        assertThat(payload.getData().getType()).isEqualTo("contact_request");
        assertThat(payload.getData().getUrl()).isEqualTo("/contacts/pending/incoming");
    }
}