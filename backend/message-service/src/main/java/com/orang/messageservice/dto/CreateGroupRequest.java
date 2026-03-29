package com.orang.messageservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.Set;
import java.util.UUID;

@Data
public class CreateGroupRequest {

    @NotBlank(message = "Group name cannot be blank")
    @Size(max = 100, message = "Group name must not exceed 100 characters")
    private String name;

    @NotEmpty(message = "Participants list cannot be empty")
    private Set<UUID> participantIds;
}
