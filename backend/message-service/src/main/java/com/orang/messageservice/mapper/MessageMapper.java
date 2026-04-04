package com.orang.messageservice.mapper;

import com.orang.messageservice.dto.MessageResponse;
import com.orang.messageservice.entity.Message;
import org.springframework.stereotype.Component;

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

        // Handle deleted messages — hide content
        if (message.isDeleted()) {
            builder.content(null);
            builder.deleted(true);
            builder.deletedAt(message.getDeletedAt());
        } else {
            builder.content(message.getContent());
            builder.deleted(false);
        }

        return builder.build();
    }
}