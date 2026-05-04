package com.orang.authservice.dto;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RefreshRequestTest {
    @Test
    void testSettersGettersAndToString() {
        RefreshRequest request = new RefreshRequest();
        
        // Test setters and getters
        request.setRefreshToken("refresh-token-abc123");
        
        assertEquals("refresh-token-abc123", request.getRefreshToken());
        
        // Test toString
        String stringRep = request.toString();
        assertNotNull(stringRep);
        assertTrue(stringRep.contains("refresh-token-abc123") || stringRep.contains("RefreshRequest"));
    }
    
    @Test
    void testEqualsAndHashCode() {
        RefreshRequest req1 = new RefreshRequest();
        req1.setRefreshToken("token-xyz");
        
        RefreshRequest req2 = new RefreshRequest();
        req2.setRefreshToken("token-xyz");
        
        assertEquals(req1, req2);
        assertEquals(req1.hashCode(), req2.hashCode());
        
        req2.setRefreshToken("token-abc");
        assertNotEquals(req1, req2);
    }
    
    @Test
    void testNullToken() {
        RefreshRequest req = new RefreshRequest();
        assertNull(req.getRefreshToken());
        req.setRefreshToken("token");
        assertEquals("token", req.getRefreshToken());
    }
}
