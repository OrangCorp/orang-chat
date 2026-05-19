package com.orang.messageservice.mapper;

import com.orang.messageservice.dto.MessageResponse;
import com.orang.messageservice.entity.Attachment;
import com.orang.messageservice.entity.Conversation;
import com.orang.messageservice.entity.FileType;
import com.orang.messageservice.entity.Message;
import com.orang.messageservice.entity.MessageReaction;
import com.orang.messageservice.entity.ReactionType;
import com.orang.messageservice.repository.MessageMentionRepository;
import com.orang.messageservice.repository.MessageReactionRepository;
import com.orang.messageservice.repository.MessageRepository;
import com.orang.messageservice.service.ReactionService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MessageMapperCoverageTest {

    @Mock
    private MessageRepository messageRepository;

    @Mock
    private MessageMentionRepository messageMentionRepository;

    @Mock
    private ReactionService reactionService;

    @Mock
    private MessageReactionRepository messageReactionRepository;

    @InjectMocks
    private MessageMapper messageMapper;

    @Test
    @DisplayName("toMessageResponse maps active messages with attachments, reply preview, mentions and reactions")
    void toMessageResponse_MapsActiveMessage() {
        UUID messageId = UUID.randomUUID();
        UUID conversationId = UUID.randomUUID();
        UUID senderId = UUID.randomUUID();
        UUID requesterId = UUID.randomUUID();
        UUID replyToId = UUID.randomUUID();

        Attachment keptAttachment = Attachment.builder()
                .id(UUID.randomUUID())
                .fileName("photo.jpg")
                .contentType("image/jpeg")
                .fileSize(42L)
                .storageKey("storage/photo.jpg")
                .thumbnailGenerated(true)
                .build();
        Attachment deletedAttachment = Attachment.builder()
                .id(UUID.randomUUID())
                .fileName("old.jpg")
                .contentType("image/jpeg")
                .fileSize(1L)
                .storageKey("storage/old.jpg")
                .thumbnailGenerated(false)
                .deletedAt(LocalDateTime.now())
                .build();

        Message message = Message.builder()
                .id(messageId)
                .conversationId(conversationId)
                .senderId(senderId)
                .content("hello world")
                .attachments(List.of(keptAttachment, deletedAttachment))
                .createdAt(LocalDateTime.now())
                .editedAt(LocalDateTime.now())
                .replyToMessageId(replyToId)
                .build();

        Message replyMessage = Message.builder()
                .id(replyToId)
                .senderId(requesterId)
                .content("a".repeat(120))
                .createdAt(LocalDateTime.now())
                .build();

        MessageReaction reaction = MessageReaction.builder()
                .id(UUID.randomUUID())
                .userId(senderId)
                .reactionType(ReactionType.ORANG)
                .createdAt(LocalDateTime.now())
                .build();

        when(messageRepository.findById(replyToId)).thenReturn(java.util.Optional.of(replyMessage));
        when(messageMentionRepository.findMentionedUserIdsByMessageId(messageId)).thenReturn(List.of(requesterId));
        when(reactionService.getReactionCounts(messageId)).thenReturn(Map.of(ReactionType.ORANG, 2L));
        when(messageReactionRepository.findByMessageId(messageId)).thenReturn(List.of(reaction));

        MessageResponse response = messageMapper.toMessageResponse(message, requesterId);

        assertThat(response.getId()).isEqualTo(messageId);
        assertThat(response.getConversationId()).isEqualTo(conversationId);
        assertThat(response.getSenderId()).isEqualTo(senderId);
        assertThat(response.isEdited()).isTrue();
        assertThat(response.getContent()).isEqualTo("hello world");
        assertThat(response.getAttachments()).hasSize(1);
        assertThat(response.getAttachments().get(0).getId()).isEqualTo(keptAttachment.getId());
        assertThat(response.getAttachments().get(0).isThumbnailAvailable()).isTrue();
        assertThat(response.getAttachments().get(0).getThumbnailUrl()).contains(keptAttachment.getId().toString());
        assertThat(response.getReplyTo()).isNotNull();
        assertThat(response.getReplyTo().getMessageId()).isEqualTo(replyToId);
        assertThat(response.getReplyTo().getContentPreview()).isEqualTo("a".repeat(100) + "...");
        assertThat(response.getMentionedUserIds()).containsExactly(requesterId);
        assertThat(response.getReactionCounts()).containsEntry(ReactionType.ORANG, 2L);
        assertThat(response.getReactions()).hasSize(1);
        assertThat(response.getReactions().get(0).getReactionType()).isEqualTo(ReactionType.ORANG);
    }

    @Test
    @DisplayName("toMessageResponse maps deleted messages and reply previews for deleted originals")
    void toMessageResponse_MapsDeletedMessage() {
        UUID messageId = UUID.randomUUID();
        UUID conversationId = UUID.randomUUID();
        UUID senderId = UUID.randomUUID();
        UUID replyToId = UUID.randomUUID();

        Message deletedMessage = Message.builder()
                .id(messageId)
                .conversationId(conversationId)
                .senderId(senderId)
                .content("secret")
                .deletedAt(LocalDateTime.now())
                .replyToMessageId(replyToId)
                .build();

        Message replyMessage = Message.builder()
                .id(replyToId)
                .senderId(senderId)
                .deletedAt(LocalDateTime.now())
                .build();

        when(messageRepository.findById(replyToId)).thenReturn(java.util.Optional.of(replyMessage));
        when(messageMentionRepository.findMentionedUserIdsByMessageId(messageId)).thenReturn(List.of());
        when(reactionService.getReactionCounts(messageId)).thenReturn(Map.of());
        when(messageReactionRepository.findByMessageId(messageId)).thenReturn(List.of());

        MessageResponse response = messageMapper.toMessageResponse(deletedMessage, senderId);

        assertThat(response.isDeleted()).isTrue();
        assertThat(response.getContent()).isNull();
        assertThat(response.getDeletedAt()).isNotNull();
        assertThat(response.getAttachments()).isEmpty();
        assertThat(response.getReplyTo().getContentPreview()).isEqualTo("[deleted]");
        assertThat(response.getReactions()).isEmpty();
    }

    @Test
    @DisplayName("attachment and entity helpers expose expected behavior")
    void attachmentAndEntityHelpers_ExposeExpectedBehavior() {
        UUID attachmentId = UUID.randomUUID();
        UUID messageId = UUID.randomUUID();
        UUID conversationId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        Attachment attachment = Attachment.builder()
                .id(attachmentId)
                .conversationId(conversationId)
                .uploaderId(userId)
                .fileName("doc.pdf")
                .contentType("application/pdf")
                .fileSize(12L)
                .storageKey("storage/doc.pdf")
                .thumbnailAttempts(0)
                .uploadedAt(LocalDateTime.now().minusHours(2))
                .build();

        assertThat(attachment.isDeleted()).isFalse();
        assertThat(attachment.isExpired()).isFalse();
        assertThat(attachment.getFileType()).isEqualTo(FileType.DOCUMENT);
        assertThat(attachment.isOrphaned(1)).isTrue();

        attachment.softDelete();
        assertThat(attachment.isDeleted()).isTrue();

        attachment.linkToMessage(messageId);
        assertThat(attachment.getMessageId()).isEqualTo(messageId);
        assertThatThrownBy(() -> attachment.linkToMessage(UUID.randomUUID()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining(messageId.toString());

        attachment.recordThumbnailAttempt("timeout");
        assertThat(attachment.getThumbnailAttempts()).isEqualTo(1);
        assertThat(attachment.getThumbnailError()).isEqualTo("timeout");
        attachment.markThumbnailSuccess("thumb-key");
        assertThat(attachment.getThumbnailGenerated()).isTrue();
        assertThat(attachment.getThumbnailStorageKey()).isEqualTo("thumb-key");
        assertThat(attachment.canRetryThumbnail()).isFalse();

        assertThatThrownBy(() -> Attachment.builder()
                .contentType("application/x-unknown")
                .build()
                .getFileType())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unsupported file type");

        assertThatThrownBy(() -> attachment.isOrphaned(-1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Grace period must be non-negative");

        Message message = Message.builder()
                .id(messageId)
                .conversationId(conversationId)
                .senderId(userId)
                .content("content")
                .attachments(new ArrayList<>())
                .build();

        message.markAsEdited();
        message.softDelete(userId);
        message.addAttachment(attachment);

        assertThat(message.isEdited()).isTrue();
        assertThat(message.isDeleted()).isTrue();
        assertThat(message.isReply()).isFalse();
        assertThat(message.getDeletedBy()).isEqualTo(userId);
        assertThat(message.getAttachments()).contains(attachment);

        Conversation conversation = Conversation.builder()
                .id(conversationId)
                .type(Conversation.ConversationType.GROUP)
                .createdBy(userId)
                .build();
        conversation.getParticipants().add(com.orang.messageservice.entity.ConversationParticipant.builder()
                .id(com.orang.messageservice.entity.ConversationParticipantId.builder()
                        .conversationId(conversationId)
                        .userId(userId)
                        .build())
                .conversation(conversation)
                .role(com.orang.messageservice.entity.ConversationParticipant.ParticipantRole.MEMBER)
                .build());

        assertThat(conversation.getParticipantIds()).containsExactly(userId);
        assertThat(Conversation.ConversationType.values()).containsExactly(
                Conversation.ConversationType.DIRECT,
                Conversation.ConversationType.GROUP
        );
        assertThat(FileType.IMAGE.supports("image/jpeg")).isTrue();
        assertThat(FileType.fromMimeType("image/jpeg")).contains(FileType.IMAGE);
        assertThat(FileType.isSupported("audio/mp4")).isTrue();
        assertThat(ReactionType.ORANG.getEmoji()).isEqualTo("🍊");
    }
}