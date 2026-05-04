package com.orang.authservice.dto;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LoginRequestTest {
    @Test
    void testSettersGettersAndToString() {
        LoginRequest request = new LoginRequest();
        
        // Test setters and getters
        request.setEmail("test@example.com");
        request.setPassword("password123");
        
        assertEquals("test@example.com", request.getEmail());
        assertEquals("password123", request.getPassword());
        
        // Test toString
        String stringRep = request.toString();
        assertNotNull(stringRep);
        assertTrue(stringRep.contains("test@example.com") || stringRep.contains("LoginRequest"));
    }
    
    @Test
    void testEqualsAndHashCode() {
        LoginRequest request1 = new LoginRequest();
        request1.setEmail("test@example.com");
        request1.setPassword("password123");
        
        LoginRequest request2 = new LoginRequest();
        request2.setEmail("test@example.com");
        request2.setPassword("password123");
        
        assertEquals(request1, request2);
        assertEquals(request1.hashCode(), request2.hashCode());
        
        // Test inequality
        request2.setEmail("different@example.com");
        assertNotEquals(request1, request2);
    }
    
    @Test
    void testNullFields() {
        LoginRequest request = new LoginRequest();
        
        assertNull(request.getEmail());
        assertNull(request.getPassword());
        
        request.setEmail("test@example.com");
        assertNotNull(request.getEmail());
        
        request.setPassword(null);
        assertNull(request.getPassword());
    }
}
