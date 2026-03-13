package com.orang.shared.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserRegisteredEvent {

    private UUID userId;
    private String displayName;

    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();
}
