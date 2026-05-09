package com.orang.authservice.dto;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ResetPasswordRequestTest {
    @Test
    void testSettersGettersAndToString() {
        ResetPasswordRequest request = new ResetPasswordRequest();
        
        // Test setters and getters
        request.setToken("reset-token-xyz123");
        request.setNewPassword("newPassword123");
        
        assertEquals("reset-token-xyz123", request.getToken());
        assertEquals("newPassword123", request.getNewPassword());
        
        // Test toString
        String stringRep = request.toString();
        assertNotNull(stringRep);
        assertTrue(stringRep.contains("ResetPasswordRequest"));
    }
    
    @Test
    void testEqualsAndHashCode() {
        ResetPasswordRequest req1 = new ResetPasswordRequest();
        req1.setToken("token-123");
        req1.setNewPassword("NewPass456");
        
        ResetPasswordRequest req2 = new ResetPasswordRequest();
        req2.setToken("token-123");
        req2.setNewPassword("NewPass456");
        
        assertEquals(req1, req2);
        assertEquals(req1.hashCode(), req2.hashCode());
        
        req2.setToken("different-token");
        assertNotEquals(req1, req2);
    }
}
