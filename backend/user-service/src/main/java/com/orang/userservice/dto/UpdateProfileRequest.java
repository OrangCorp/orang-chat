package com.orang.userservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateProfileRequest {

    @Size(min = 2, max = 50, message = "Display name must be between 2 and 50 characters")
    private String displayName;

    @Size(max = 500, message = "Avatar URL must be less than 500 characters")
    private String avatarUrl;

    @Size(max = 500, message = "Bio must be less than 500 characters")
    private String bio;
}
