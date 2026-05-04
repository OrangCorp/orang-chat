package com.orang.authservice.dto;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ForgotPasswordRequestTest {
    @Test
    void testSettersGettersAndToString() {
        ForgotPasswordRequest request = new ForgotPasswordRequest();
        
        // Test setters and getters
        request.setEmail("test@example.com");
        
        assertEquals("test@example.com", request.getEmail());
        
        // Test toString
        String stringRep = request.toString();
        assertNotNull(stringRep);
        assertTrue(stringRep.contains("test@example.com") || stringRep.contains("ForgotPasswordRequest"));
    }
    
    @Test
    void testEqualsAndHashCode() {
        ForgotPasswordRequest req1 = new ForgotPasswordRequest();
        req1.setEmail("user@test.com");
        
        ForgotPasswordRequest req2 = new ForgotPasswordRequest();
        req2.setEmail("user@test.com");
        
        assertEquals(req1, req2);
        assertEquals(req1.hashCode(), req2.hashCode());
        
        req2.setEmail("other@test.com");
        assertNotEquals(req1, req2);
    }
}
