package com.orang.authservice.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegisterRequest {

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    @Size(min = 5, max = 100, message = "Email must be between 5 and 100 characters long")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters long")
    private String password;

    @NotBlank(message = "Display name is required")
    @Size(min = 2, max = 50, message = "Display name must be between 2 and 50 characters")
    private String displayName;
}
