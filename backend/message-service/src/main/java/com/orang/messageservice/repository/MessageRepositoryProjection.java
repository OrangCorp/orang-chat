package com.orang.messageservice.repository;

import java.time.LocalDateTime;
import java.util.UUID;

public interface MessageRepositoryProjection {

    UUID getId();
    UUID getConversationId();
    UUID getSenderId();
    String getContent();
    String getHighlightedContent();
    Float getRank();
    LocalDateTime getCreatedAt();
}
