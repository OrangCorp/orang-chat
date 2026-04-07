package com.orang.shared.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ThumbnailReadyEvent implements Serializable {

    private UUID attachmentId;
    private UUID conversationId;
    private UUID messageId;
    private String thumbnailUrl;
}