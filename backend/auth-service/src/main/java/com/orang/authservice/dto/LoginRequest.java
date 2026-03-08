package com.orang.authservice.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class LoginRequest {

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    @Size(min = 5, max = 100)
    private String email;

    @NotBlank(message = "Password is required")
    private String password;
}
