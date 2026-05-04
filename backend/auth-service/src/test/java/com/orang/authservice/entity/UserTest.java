package com.orang.authservice.entity;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("User Entity Tests")
class UserTest {

    @Test
    @DisplayName("Should create user with builder")
    void testUserBuilder() {
        UUID userId = UUID.randomUUID();
        String email = "test@example.com";
        String passwordHash = "hashed";
        String displayName = "Test User";

        User user = User.builder()
                .id(userId)
                .email(email)
                .passwordHash(passwordHash)
                .displayName(displayName)
                .emailVerified(true)
                .build();

        assertThat(user.getId()).isEqualTo(userId);
        assertThat(user.getEmail()).isEqualTo(email);
        assertThat(user.getPasswordHash()).isEqualTo(passwordHash);
        assertThat(user.getDisplayName()).isEqualTo(displayName);
        assertThat(user.isEmailVerified()).isTrue();
    }

    @Test
    @DisplayName("Should set and get email")
    void testEmailSetterGetter() {
        User user = new User();
        user.setEmail("new@example.com");
        assertThat(user.getEmail()).isEqualTo("new@example.com");
    }

    @Test
    @DisplayName("Should set and get password hash")
    void testPasswordHashSetterGetter() {
        User user = new User();
        user.setPasswordHash("new-hash");
        assertThat(user.getPasswordHash()).isEqualTo("new-hash");
    }

    @Test
    @DisplayName("Should set and get display name")
    void testDisplayNameSetterGetter() {
        User user = new User();
        user.setDisplayName("New Name");
        assertThat(user.getDisplayName()).isEqualTo("New Name");
    }

    @Test
    @DisplayName("Should set and get email verified status")
    void testEmailVerifiedSetterGetter() {
        User user = new User();
        user.setEmailVerified(true);
        assertThat(user.isEmailVerified()).isTrue();
        
        user.setEmailVerified(false);
        assertThat(user.isEmailVerified()).isFalse();
    }

    @Test
    @DisplayName("Should set and get ID")
    void testIdSetterGetter() {
        User user = new User();
        UUID id = UUID.randomUUID();
        user.setId(id);
        assertThat(user.getId()).isEqualTo(id);
    }

    @Test
    @DisplayName("Should handle user instances")
    void testUserInstances() {
        UUID id = UUID.randomUUID();
        
        User user1 = User.builder()
                .id(id)
                .email("test@example.com")
                .passwordHash("hash")
                .displayName("User")
                .emailVerified(true)
                .build();
        
        User user2 = new User();
        user2.setId(id);
        user2.setEmail("test@example.com");
        
        assertThat(user1.getId()).isEqualTo(user2.getId());
        assertThat(user1.getEmail()).isEqualTo(user2.getEmail());
    }

    @Test
    @DisplayName("Should test toString")
    void testToString() {
        User user = new User();
        user.setEmail("test@example.com");
        
        String toString = user.toString();
        assertThat(toString).isNotNull().contains("User");
    }
}
