package com.orang.authservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class RegistrationResponse {

    private UUID userId;
    private String email;
    private String displayName;
    private boolean emailVerified;
    
    // Optional warning message if email failed to send
    private String warning;
}
