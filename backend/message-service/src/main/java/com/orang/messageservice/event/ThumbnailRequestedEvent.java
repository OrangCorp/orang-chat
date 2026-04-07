package com.orang.messageservice.event;

import com.orang.messageservice.entity.FileType;
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
public class ThumbnailRequestedEvent implements Serializable {

    private UUID attachmentId;
    private UUID conversationId;
    private String storageKey;
    private FileType fileType;
}
