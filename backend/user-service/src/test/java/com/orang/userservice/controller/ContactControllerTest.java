package com.orang.userservice.controller;

import com.orang.userservice.dto.ContactResponse;
import com.orang.userservice.entity.Contact;
import com.orang.userservice.entity.ContactStatus;
import com.orang.userservice.service.ContactService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ContactControllerTest {

    @Mock
    private ContactService contactService;

    private ContactController contactController;

    private static final String USER_ID = "844ec9f6-f781-4f67-aab0-1f33cf9734f7";
    private static final UUID TARGET_ID = UUID.fromString("bc9e3f0b-8c33-4ef0-8c8d-b1d0f3f8f9d2");
    private static final UUID CONTACT_ID = UUID.fromString("2f3c4f2d-2eb3-49f9-9db8-0f5b5ff1a111");

    @BeforeEach
    void setUp() {
        contactController = new ContactController(contactService);
    }

    @Test
    @DisplayName("sendRequest returns created contact response")
    void sendRequestReturnsCreatedContactResponse() {
        when(contactService.sendRequest(UUID.fromString(USER_ID), TARGET_ID)).thenReturn(contact(ContactStatus.PENDING));

        ResponseEntity<ContactResponse> response = contactController.sendRequest(USER_ID, TARGET_ID);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getId()).isEqualTo(CONTACT_ID);
    }

    @Test
    @DisplayName("blockUser returns contact response")
    void blockUserReturnsContactResponse() {
        when(contactService.blockUser(UUID.fromString(USER_ID), TARGET_ID)).thenReturn(contact(ContactStatus.BLOCKED));

        ResponseEntity<ContactResponse> response = contactController.blockUser(USER_ID, TARGET_ID);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(ContactStatus.BLOCKED);
    }

    @Test
    @DisplayName("unblockUser returns no content")
    void unblockUserReturnsNoContent() {
        ResponseEntity<Void> response = contactController.unblockUser(USER_ID, TARGET_ID);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }

    @Test
    @DisplayName("acceptRequest returns accepted contact response")
    void acceptRequestReturnsAcceptedContactResponse() {
        when(contactService.acceptRequest(UUID.fromString(USER_ID), CONTACT_ID)).thenReturn(contact(ContactStatus.ACCEPTED));

        ResponseEntity<ContactResponse> response = contactController.acceptRequest(USER_ID, CONTACT_ID);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(ContactStatus.ACCEPTED);
    }

    @Test
    @DisplayName("rejectRequest returns no content")
    void rejectRequestReturnsNoContent() {
        ResponseEntity<Void> response = contactController.rejectRequest(USER_ID, CONTACT_ID);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }

    @Test
    @DisplayName("cancelRequest returns no content")
    void cancelRequestReturnsNoContent() {
        ResponseEntity<Void> response = contactController.cancelRequest(USER_ID, CONTACT_ID);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }

    @Test
    @DisplayName("removeContact returns no content")
    void removeContactReturnsNoContent() {
        ResponseEntity<Void> response = contactController.removeContact(USER_ID, CONTACT_ID);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }

    @Test
    @DisplayName("getContacts returns mapped responses")
    void getContactsReturnsMappedResponses() {
        when(contactService.getAcceptedContacts(UUID.fromString(USER_ID))).thenReturn(List.of(contact(ContactStatus.ACCEPTED)));

        ResponseEntity<List<ContactResponse>> response = contactController.getContacts(USER_ID);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(1);
        assertThat(response.getBody().getFirst().getId()).isEqualTo(CONTACT_ID);
    }

    @Test
    @DisplayName("getIncomingRequests returns mapped responses")
    void getIncomingRequestsReturnsMappedResponses() {
        when(contactService.getIncomingRequests(UUID.fromString(USER_ID))).thenReturn(List.of(contact(ContactStatus.PENDING)));

        ResponseEntity<List<ContactResponse>> response = contactController.getIncomingRequests(USER_ID);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(1);
    }

    @Test
    @DisplayName("getOutgoingRequests returns mapped responses")
    void getOutgoingRequestsReturnsMappedResponses() {
        when(contactService.getOutgoingRequests(UUID.fromString(USER_ID))).thenReturn(List.of(contact(ContactStatus.PENDING)));

        ResponseEntity<List<ContactResponse>> response = contactController.getOutgoingRequests(USER_ID);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(1);
    }

    @Test
    @DisplayName("getBlockedUsers returns mapped responses")
    void getBlockedUsersReturnsMappedResponses() {
        when(contactService.getBlockedUsers(UUID.fromString(USER_ID))).thenReturn(List.of(contact(ContactStatus.BLOCKED)));

        ResponseEntity<List<ContactResponse>> response = contactController.getBlockedUsers(USER_ID);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(1);
        assertThat(response.getBody().getFirst().getStatus()).isEqualTo(ContactStatus.BLOCKED);
    }

    private Contact contact(ContactStatus status) {
        return Contact.builder()
                .id(CONTACT_ID)
                .requesterId(UUID.fromString(USER_ID))
                .recipientId(TARGET_ID)
                .status(status)
                .acceptedAt(status == ContactStatus.ACCEPTED ? LocalDateTime.of(2026, 5, 4, 11, 0) : null)
                .createdAt(LocalDateTime.of(2026, 5, 4, 10, 0))
                .build();
    }
}