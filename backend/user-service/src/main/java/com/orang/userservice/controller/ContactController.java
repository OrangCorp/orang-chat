package com.orang.userservice.controller;

import com.orang.userservice.dto.ContactResponse;
import com.orang.userservice.entity.Contact;
import com.orang.userservice.service.ContactService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/contacts")
@RequiredArgsConstructor
public class ContactController {

    private final ContactService contactService;

    // ==================== Target User ID Actions ====================

    @PostMapping("/request/{targetUserId}")
    public ResponseEntity<ContactResponse> sendRequest(
            @AuthenticationPrincipal String userId,
            @PathVariable UUID targetUserId) {
        Contact contact = contactService.sendRequest(UUID.fromString(userId), targetUserId);
        return ResponseEntity.status(HttpStatus.CREATED).body(toContactResponse(contact));
    }

    @PostMapping("/block/{targetUserId}")
    public ResponseEntity<ContactResponse> blockUser(
            @AuthenticationPrincipal String userId,
            @PathVariable UUID targetUserId) {
        Contact contact = contactService.blockUser(UUID.fromString(userId), targetUserId);
        return ResponseEntity.ok(toContactResponse(contact));
    }

    @DeleteMapping("/block/{targetUserId}")
    public ResponseEntity<Void> unblockUser(
            @AuthenticationPrincipal String userId,
            @PathVariable UUID targetUserId) {
        contactService.unblockUser(UUID.fromString(userId), targetUserId);
        return ResponseEntity.noContent().build();
    }

    // ==================== Contact ID Actions ====================

    @PostMapping("/{contactId}/accept")
    public ResponseEntity<ContactResponse> acceptRequest(
            @AuthenticationPrincipal String userId,
            @PathVariable UUID contactId) {
        Contact contact = contactService.acceptRequest(UUID.fromString(userId), contactId);
        return ResponseEntity.ok(toContactResponse(contact));
    }

    @PostMapping("/{contactId}/reject")
    public ResponseEntity<Void> rejectRequest(
            @AuthenticationPrincipal String userId,
            @PathVariable UUID contactId) {
        contactService.rejectRequest(UUID.fromString(userId), contactId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{contactId}/cancel")
    public ResponseEntity<Void> cancelRequest(
            @AuthenticationPrincipal String userId,
            @PathVariable UUID contactId) {
        contactService.cancelRequest(UUID.fromString(userId), contactId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{contactId}")
    public ResponseEntity<Void> removeContact(
            @AuthenticationPrincipal String userId,
            @PathVariable UUID contactId) {
        contactService.removeContact(UUID.fromString(userId), contactId);
        return ResponseEntity.noContent().build();
    }

    // ==================== Queries ====================

    @GetMapping
    public ResponseEntity<List<ContactResponse>> getContacts(
            @AuthenticationPrincipal String userId) {
        List<ContactResponse> contacts = contactService.getAcceptedContacts(UUID.fromString(userId))
                .stream()
                .map(this::toContactResponse)
                .toList();
        return ResponseEntity.ok(contacts);
    }

    @GetMapping("/pending/incoming")
    public ResponseEntity<List<ContactResponse>> getIncomingRequests(
            @AuthenticationPrincipal String userId) {
        List<ContactResponse> contacts = contactService.getIncomingRequests(UUID.fromString(userId))
                .stream()
                .map(this::toContactResponse)
                .toList();
        return ResponseEntity.ok(contacts);
    }

    @GetMapping("/pending/outgoing")
    public ResponseEntity<List<ContactResponse>> getOutgoingRequests(
            @AuthenticationPrincipal String userId) {
        List<ContactResponse> contacts = contactService.getOutgoingRequests(UUID.fromString(userId))
                .stream()
                .map(this::toContactResponse)
                .toList();
        return ResponseEntity.ok(contacts);
    }

    @GetMapping("/blocked")
    public ResponseEntity<List<ContactResponse>> getBlockedUsers(
            @AuthenticationPrincipal String userId) {
        List<ContactResponse> contacts = contactService.getBlockedUsers(UUID.fromString(userId))
                .stream()
                .map(this::toContactResponse)
                .toList();
        return ResponseEntity.ok(contacts);
    }

    // ==================== Mapper ====================

    private ContactResponse toContactResponse(Contact contact) {
        return ContactResponse.builder()
                .id(contact.getId())
                .requesterId(contact.getRequesterId())
                .recipientId(contact.getRecipientId())
                .status(contact.getStatus())
                .acceptedAt(contact.getAcceptedAt())
                .createdAt(contact.getCreatedAt())
                .build();
    }
}