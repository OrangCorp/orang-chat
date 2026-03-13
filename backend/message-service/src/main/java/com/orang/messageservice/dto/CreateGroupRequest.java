package com.orang.messageservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.Set;
import java.util.UUID;

@Data
public class CreateGroupRequest {

    @NotBlank(message = "Group name cannot be blank")
    private String name;

    @NotEmpty(message = "Participants list cannot be empty")
    private Set<UUID> participantIds;
}
