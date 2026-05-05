package com.orang.userservice.listener;

import com.orang.shared.event.UserRegisteredEvent;
import com.orang.userservice.service.ProfileService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class UserEventListenerTest {

    @Mock
    private ProfileService profileService;

    @Test
    @DisplayName("userRegisteredEvent delegates to profile service")
    void userRegisteredEventDelegatesToProfileService() {
        UserEventListener listener = new UserEventListener(profileService);
        UserRegisteredEvent event = new UserRegisteredEvent(
                UUID.fromString("844ec9f6-f781-4f67-aab0-1f33cf9734f7"),
                "Alice",
                LocalDateTime.of(2026, 5, 4, 10, 0)
        );

        listener.userRegisteredEvent(event);

        verify(profileService).createProfileIfNotExists(event.getUserId(), event.getDisplayName());
    }

    @Test
    @DisplayName("userRegisteredEvent wraps failures for retry handling")
    void userRegisteredEventWrapsFailures() {
        UserEventListener listener = new UserEventListener(profileService);
        UserRegisteredEvent event = new UserRegisteredEvent(
                UUID.fromString("844ec9f6-f781-4f67-aab0-1f33cf9734f7"),
                "Alice",
                LocalDateTime.of(2026, 5, 4, 10, 0)
        );

        doThrow(new IllegalStateException("boom"))
                .when(profileService)
                .createProfileIfNotExists(event.getUserId(), event.getDisplayName());

        assertThatThrownBy(() -> listener.userRegisteredEvent(event))
                .isInstanceOf(AmqpRejectAndDontRequeueException.class)
                .hasMessageContaining("Failed to process UserRegisteredEvent");
    }
}