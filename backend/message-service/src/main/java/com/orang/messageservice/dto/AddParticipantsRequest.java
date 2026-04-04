package com.orang.messageservice.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AddParticipantsRequest {
    @NotEmpty(message = "At least one participant is required")
    private Set<UUID> userIds;
}
