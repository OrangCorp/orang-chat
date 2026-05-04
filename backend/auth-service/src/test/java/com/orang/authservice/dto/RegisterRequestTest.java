package com.orang.authservice.dto;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RegisterRequestTest {

    @Test
    void settersGettersAndToString() {
        RegisterRequest req = new RegisterRequest();
        req.setEmail("x@y.com");
        req.setPassword("password123");
        req.setDisplayName("Display");

        assertEquals("x@y.com", req.getEmail());
        assertEquals("password123", req.getPassword());
        assertEquals("Display", req.getDisplayName());

        assertNotNull(req.toString());
        assertEquals(req.hashCode(), req.hashCode());
    }
    
    @Test
    void testEqualsAndHashCode() {
        RegisterRequest req1 = new RegisterRequest();
        req1.setEmail("test@example.com");
        req1.setPassword("password123");
        req1.setDisplayName("Test User");
        
        RegisterRequest req2 = new RegisterRequest();
        req2.setEmail("test@example.com");
        req2.setPassword("password123");
        req2.setDisplayName("Test User");
        
        assertEquals(req1, req2);
        assertEquals(req1.hashCode(), req2.hashCode());
        
        // Test inequality
        req2.setDisplayName("Different User");
        assertNotEquals(req1, req2);
    }
    
    @Test
    void testAllFieldCombinations() {
        RegisterRequest req = new RegisterRequest();
        
        req.setEmail("user@test.com");
        assertEquals("user@test.com", req.getEmail());
        
        req.setPassword("SecurePass123");
        assertEquals("SecurePass123", req.getPassword());
        
        req.setDisplayName("John Doe");
        assertEquals("John Doe", req.getDisplayName());
        
        assertNotNull(req.toString());
    }
}
