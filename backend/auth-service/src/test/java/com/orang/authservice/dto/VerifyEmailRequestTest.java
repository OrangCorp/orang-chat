package com.orang.authservice.dto;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class VerifyEmailRequestTest {
    @Test
    void testSettersGettersAndToString() {
        VerifyEmailRequest request = new VerifyEmailRequest();
        
        request.setEmail("test@example.com");
        request.setCode("123456");
        
        assertEquals("test@example.com", request.getEmail());
        assertEquals("123456", request.getCode());
        
        String stringRep = request.toString();
        assertNotNull(stringRep);
        assertTrue(stringRep.contains("test@example.com") || stringRep.contains("VerifyEmailRequest"));
    }
    
    @Test
    void testEqualsAndHashCode() {
        VerifyEmailRequest req1 = new VerifyEmailRequest();
        req1.setEmail("test@example.com");
        req1.setCode("654321");
        
        VerifyEmailRequest req2 = new VerifyEmailRequest();
        req2.setEmail("test@example.com");
        req2.setCode("654321");
        
        assertEquals(req1, req2);
        assertEquals(req1.hashCode(), req2.hashCode());
        
        req2.setCode("111111");
        assertNotEquals(req1, req2);
    }
}
