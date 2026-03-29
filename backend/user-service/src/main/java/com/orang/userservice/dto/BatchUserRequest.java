package com.orang.userservice.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BatchUserRequest {

    @NotNull(message = "User IDs cannot be null")
    @Size(min = 1, max = 100, message = "Must request between 1 and 100 users")
    private Set<UUID> userIds;
}
