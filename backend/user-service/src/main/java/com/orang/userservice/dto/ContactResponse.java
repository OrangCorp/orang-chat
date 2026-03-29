package com.orang.userservice.dto;

import com.orang.userservice.entity.ContactStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContactResponse {
    private UUID id;
    private UUID requesterId;
    private UUID recipientId;
    private ContactStatus status;
    private LocalDateTime acceptedAt;
    private LocalDateTime createdAt;
}
