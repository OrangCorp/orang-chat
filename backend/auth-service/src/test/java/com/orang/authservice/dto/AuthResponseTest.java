package com.orang.authservice.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class AuthResponseTest {

    @Test
    void builderGettersEqualsAndSerialization() throws Exception {
        UUID id = UUID.randomUUID();

        AuthResponse r = AuthResponse.builder()
                .userId(id)
                .email("a@b.com")
                .displayName("Name")
                .accessToken("acc")
                .refreshToken("ref")
                .tokenType("Bearer")
                .expiresIn(3600L)
                .emailVerified(true)
                .build();

        assertEquals(id, r.getUserId());
        assertEquals("a@b.com", r.getEmail());
        assertTrue(r.isEmailVerified());

        ObjectMapper mapper = new ObjectMapper();
        String json = mapper.writeValueAsString(r);
        assertNotNull(json);
        assertTrue(json.contains("accessToken"));

        AuthResponse same = AuthResponse.builder()
                .userId(id)
                .email("a@b.com")
                .displayName("Name")
                .accessToken("acc")
                .refreshToken("ref")
                .tokenType("Bearer")
                .expiresIn(3600L)
                .emailVerified(true)
                .build();

        assertEquals(r, same);
        assertEquals(r.hashCode(), same.hashCode());
        assertNotNull(r.toString());
    }
}
