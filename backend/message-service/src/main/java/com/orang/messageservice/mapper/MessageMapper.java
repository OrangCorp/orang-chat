package com.orang.messageservice.mapper;

import com.orang.messageservice.dto.MessageResponse;
import com.orang.messageservice.entity.Attachment;
import com.orang.messageservice.entity.Message;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Component
public class MessageMapper {

    public MessageResponse toMessageResponse(Message message) {
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

        return builder.build();
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