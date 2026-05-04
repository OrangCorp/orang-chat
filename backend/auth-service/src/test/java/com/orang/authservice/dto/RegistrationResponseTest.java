package com.orang.authservice.dto;

import org.junit.jupiter.api.Test;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class RegistrationResponseTest {
    @Test
    void testBuildersGettersEqualsAndToString() {
        UUID userId = UUID.randomUUID();
        
        // Test builder pattern
        RegistrationResponse response = RegistrationResponse.builder()
                .userId(userId)
                .email("test@example.com")
                .displayName("Test User")
                .build();
        
        assertEquals(userId, response.getUserId());
        assertEquals("test@example.com", response.getEmail());
        assertEquals("Test User", response.getDisplayName());
        
        // Test all setters
        response.setUserId(userId);
        response.setEmail("new@example.com");
        response.setDisplayName("New User");
        
        assertEquals("new@example.com", response.getEmail());
        assertEquals("New User", response.getDisplayName());
        
        // Test toString
        String stringRep = response.toString();
        assertNotNull(stringRep);
        assertTrue(stringRep.contains("new@example.com") || stringRep.contains("RegistrationResponse"));
    }
    
    @Test
    void testEqualsAndHashCode() {
        UUID userId = UUID.randomUUID();
        
        RegistrationResponse resp1 = RegistrationResponse.builder()
                .userId(userId)
                .email("test@example.com")
                .displayName("Test User")
                .build();
        
        RegistrationResponse resp2 = RegistrationResponse.builder()
                .userId(userId)
                .email("test@example.com")
                .displayName("Test User")
                .build();
        
        assertEquals(resp1, resp2);
        assertEquals(resp1.hashCode(), resp2.hashCode());
        
        resp2.setEmail("different@example.com");
        assertNotEquals(resp1, resp2);
    }
}
