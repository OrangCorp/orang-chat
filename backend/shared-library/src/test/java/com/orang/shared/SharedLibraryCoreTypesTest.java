package com.orang.shared;

import com.orang.shared.constants.PaginationConstants;
import com.orang.shared.constants.PresenceConstants;
import com.orang.shared.constants.RabbitMQConstants;
import com.orang.shared.dto.ApiResponse;
import com.orang.shared.dto.ChatMessagePayload;
import com.orang.shared.dto.MessageType;
import com.orang.shared.event.MessageSentEvent;
import com.orang.shared.event.UserRegisteredEvent;
import com.orang.shared.presence.UserStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class SharedLibraryCoreTypesTest {

    @Test
    @DisplayName("ApiResponse factory methods populate the expected fields")
    void apiResponseFactories_PopulateExpectedFields() {
        ApiResponse<String> success = ApiResponse.success("payload");
        ApiResponse<String> successWithMessage = ApiResponse.success("created", "payload");
        ApiResponse<String> error = ApiResponse.error("boom");

        assertThat(success.isSuccess()).isTrue();
        assertThat(success.getData()).isEqualTo("payload");
        assertThat(success.getTimestamp()).isNotNull();

        assertThat(successWithMessage.isSuccess()).isTrue();
        assertThat(successWithMessage.getMessage()).isEqualTo("created");
        assertThat(successWithMessage.getData()).isEqualTo("payload");
        assertThat(successWithMessage.getTimestamp()).isNotNull();

        assertThat(error.isSuccess()).isFalse();
        assertThat(error.getMessage()).isEqualTo("boom");
        assertThat(error.getTimestamp()).isNotNull();
    }

    @Test
    @DisplayName("ChatMessagePayload builder keeps all fields including default timestamp")
    void chatMessagePayloadBuilder_PopulatesFields() {
        UUID senderId = UUID.randomUUID();
        UUID recipientId = UUID.randomUUID();
        UUID conversationId = UUID.randomUUID();
        UUID messageId = UUID.randomUUID();
        UUID replyToMessageId = UUID.randomUUID();
        UUID attachmentId = UUID.randomUUID();

        ChatMessagePayload payload = ChatMessagePayload.builder()
                .senderId(senderId)
                .recipientId(recipientId)
                .conversationId(conversationId)
                .content("hello")
                .type(MessageType.GROUP)
                .attachmentIds(List.of(attachmentId))
                .messageId(messageId)
                .replyToMessageId(replyToMessageId)
                .build();

        assertThat(payload.getSenderId()).isEqualTo(senderId);
        assertThat(payload.getRecipientId()).isEqualTo(recipientId);
        assertThat(payload.getConversationId()).isEqualTo(conversationId);
        assertThat(payload.getContent()).isEqualTo("hello");
        assertThat(payload.getType()).isEqualTo(MessageType.GROUP);
        assertThat(payload.getAttachmentIds()).containsExactly(attachmentId);
        assertThat(payload.getMessageId()).isEqualTo(messageId);
        assertThat(payload.getReplyToMessageId()).isEqualTo(replyToMessageId);
        assertThat(payload.getTimestamp()).isNotNull();
    }

    @Test
    @DisplayName("Event builders populate inherited and local fields")
    void eventBuilders_PopulateInheritedAndLocalFields() {
        UUID messageId = UUID.randomUUID();
        UUID conversationId = UUID.randomUUID();
        UUID triggeredBy = UUID.randomUUID();
        UUID participantId = UUID.randomUUID();
        UUID attachmentId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        MessageSentEvent messageSentEvent = MessageSentEvent.builder()
                .messageId(messageId)
                .conversationId(conversationId)
                .triggeredBy(triggeredBy)
                .timestamp(LocalDateTime.now())
                .content("message")
                .attachmentIds(List.of(attachmentId))
                .participantIds(Set.of(participantId))
                .build();

        UserRegisteredEvent userRegisteredEvent = UserRegisteredEvent.builder()
                .userId(userId)
                .displayName("Test User")
                .build();

        assertThat(messageSentEvent.getMessageId()).isEqualTo(messageId);
        assertThat(messageSentEvent.getConversationId()).isEqualTo(conversationId);
        assertThat(messageSentEvent.getTriggeredBy()).isEqualTo(triggeredBy);
        assertThat(messageSentEvent.getContent()).isEqualTo("message");
        assertThat(messageSentEvent.getAttachmentIds()).containsExactly(attachmentId);
        assertThat(messageSentEvent.getParticipantIds()).containsExactly(participantId);

        assertThat(userRegisteredEvent.getUserId()).isEqualTo(userId);
        assertThat(userRegisteredEvent.getDisplayName()).isEqualTo("Test User");
        assertThat(userRegisteredEvent.getTimestamp()).isNotNull();
    }

    @Test
    @DisplayName("Constants and enums expose the expected values")
    void constantsAndEnums_ExposeExpectedValues() {
        assertThat(PresenceConstants.userSessionsKey("user-1")).isEqualTo("user:user-1:sessions");
        assertThat(PresenceConstants.userLastActivityKey("user-1")).isEqualTo("user:user-1:lastActivity");
        assertThat(PresenceConstants.sessionMetaKey("session-1")).isEqualTo("session:session-1:meta");
        assertThat(PresenceConstants.ONLINE_THRESHOLD_SECONDS).isEqualTo(120L);
        assertThat(PresenceConstants.AWAY_THRESHOLD_SECONDS).isEqualTo(600L);

        assertThat(RabbitMQConstants.MESSAGE_EXCHANGE).isEqualTo("message.exchange");
        assertThat(RabbitMQConstants.USER_EXCHANGE).isEqualTo("user.exchange");
        assertThat(RabbitMQConstants.NOTIFICATION_QUEUE).isEqualTo("notification.queue");

        assertThat(PaginationConstants.DEFAULT_PAGE_NUMBER).isZero();
        assertThat(PaginationConstants.DEFAULT_PAGE_SIZE).isEqualTo(50);
        assertThat(PaginationConstants.MAX_PAGE_SIZE).isEqualTo(100);
        assertThat(PaginationConstants.MIN_PAGE_SIZE).isEqualTo(1);

        assertThat(UserStatus.values()).containsExactly(UserStatus.ONLINE, UserStatus.AWAY, UserStatus.OFFLINE);
        assertThat(MessageType.values()).containsExactly(MessageType.DIRECT, MessageType.GROUP, MessageType.TYPING);
    }
}