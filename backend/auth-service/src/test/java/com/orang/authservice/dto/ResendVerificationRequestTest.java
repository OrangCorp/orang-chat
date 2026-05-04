package com.orang.authservice.dto;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ResendVerificationRequestTest {
    @Test
    void testSettersGettersAndToString() {
        ResendVerificationRequest request = new ResendVerificationRequest();
        
        // Test setters and getters
        request.setEmail("test@example.com");
        
        assertEquals("test@example.com", request.getEmail());
        
        // Test toString
        String stringRep = request.toString();
        assertNotNull(stringRep);
        assertTrue(stringRep.contains("test@example.com") || stringRep.contains("ResendVerificationRequest"));
    }
    
    @Test
    void testEqualsAndHashCode() {
        ResendVerificationRequest req1 = new ResendVerificationRequest();
        req1.setEmail("verify@test.com");
        
        ResendVerificationRequest req2 = new ResendVerificationRequest();
        req2.setEmail("verify@test.com");
        
        assertEquals(req1, req2);
        assertEquals(req1.hashCode(), req2.hashCode());
        
        req2.setEmail("different@test.com");
        assertNotEquals(req1, req2);
    }
}
