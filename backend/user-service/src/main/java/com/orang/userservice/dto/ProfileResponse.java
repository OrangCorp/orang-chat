package com.orang.userservice.dto;

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
public class ProfileResponse {

    private UUID userId;
    private String displayName;
    private String avatarUrl;
    private String bio;
    private LocalDateTime lastSeen;
    private boolean isOnline;
}
