package com.orang.messageservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessagesAroundResponse {

    private List<MessageResponse> messages;
    private UUID targetMessageId;

    /**
     * Index of target message in the messages list.
     *
     * Example:
     *   If messages = [msg1, msg2, TARGET, msg3, msg4]
     *   Then targetIndex = 2
     *
     * Frontend can scroll to messages[targetIndex] to highlight it.
     */
    private Integer targetIndex;
    private Boolean hasOlderMessages;
    private Boolean hasNewerMessages;
}
