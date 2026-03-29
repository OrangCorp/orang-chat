package com.orang.userservice.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BatchStatusRequest {

    @NotEmpty(message = "User IDs list cannot be empty")
    @Size(max = 100, message = "Cannot check more than 100 users at once")
    private List<String> userIds;
}
