package com.orang.messageservice.mapper;

import com.orang.messageservice.dto.MessageResponse;
import com.orang.messageservice.entity.Attachment;
import com.orang.messageservice.entity.Message;
import com.orang.messageservice.repository.MessageMentionRepository;
import com.orang.messageservice.repository.MessageRepository;
import com.orang.messageservice.repository.MessageReactionRepository;
import com.orang.messageservice.service.ReactionService;
import com.orang.messageservice.entity.ReactionType;
import com.orang.messageservice.entity.MessageReaction;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class MessageMapper {

    private final MessageRepository messageRepository;
    private final MessageMentionRepository messageMentionRepository;
    private final ReactionService reactionService;
    private final MessageReactionRepository messageReactionRepository;

    public MessageResponse toMessageResponse(Message message, java.util.UUID requesterId) {
        MessageResponse.MessageResponseBuilder builder = MessageResponse.builder()
                .id(message.getId())
                .conversationId(message.getConversationId())
                .senderId(message.getSenderId())
                .createdAt(message.getCreatedAt())
                .edited(message.isEdited())
                .editedAt(message.getEditedAt());

        if (message.isDeleted()) {
            builder.content(null);
            builder.deleted(true);
            builder.deletedAt(message.getDeletedAt());
            builder.attachments(Collections.emptyList());
        } else {
            builder.content(message.getContent());
            builder.deleted(false);
            builder.attachments(toAttachmentInfoList(message.getAttachments()));
        }

        if (message.getReplyToMessageId() != null) {
            messageRepository.findById(message.getReplyToMessageId())
                    .ifPresent(original -> {
                        builder.replyTo(MessageResponse.ReplyPreview.builder()
                                .messageId(original.getId())
                                .senderId(original.getSenderId())
                                .contentPreview(buildPreview(original))
                                .deleted(original.isDeleted())
                                .build());
                    });
        }

        List<UUID> mentionedIds = messageMentionRepository
                .findMentionedUserIdsByMessageId(message.getId());
        builder.mentionedUserIds(mentionedIds);

        Map<ReactionType, Long> counts = reactionService.getReactionCounts(message.getId());
        builder.reactionCounts(counts);

        // `myReaction` removed — full reaction entries are available in `reactions` list.

        List<MessageReaction> reactions = messageReactionRepository.findByMessageId(message.getId());
        if (reactions != null && !reactions.isEmpty()) {
            builder.reactions(reactions.stream().map(r -> MessageResponse.ReactionInfo.builder()
                .id(r.getId())
                .userId(r.getUserId())
                .reactionType(r.getReactionType())
                .createdAt(r.getCreatedAt())
                .build()).collect(Collectors.toList()));
        } else {
            builder.reactions(java.util.Collections.emptyList());
        }

        return builder.build();
    }

    private String buildPreview(Message original) {
        if (original.isDeleted()) {
            return "[deleted]";
        }

        String content = original.getContent();
        if (content == null || content.isBlank()) {
            return "[Attachment]";
        }

        if (content.length() <= 100) {
            return content;
        }

        return content.substring(0, 100) + "...";
    }

    private List<MessageResponse.AttachmentInfo> toAttachmentInfoList(List<Attachment> attachments) {
        if (attachments == null || attachments.isEmpty()) {
            return Collections.emptyList();
        }

        return attachments.stream()
                .filter(a -> !a.isDeleted())
                .map(this::toAttachmentInfo)
                .toList();
    }

    private MessageResponse.AttachmentInfo toAttachmentInfo(Attachment attachment) {
        String thumbnailUrl = null;
        if (Boolean.TRUE.equals(attachment.getThumbnailGenerated())) {
            thumbnailUrl = "/api/attachments/" + attachment.getId() + "/thumbnail";
        }

        return MessageResponse.AttachmentInfo.builder()
                .id(attachment.getId())
                .fileName(attachment.getFileName())
                .contentType(attachment.getContentType())
                .fileSize(attachment.getFileSize())
                .fileType(attachment.getFileType())
                .downloadUrl("/api/attachments/" + attachment.getId() + "/download")
                .thumbnailAvailable(Boolean.TRUE.equals(attachment.getThumbnailGenerated()))
                .thumbnailUrl(thumbnailUrl)
                .build();
    }
}