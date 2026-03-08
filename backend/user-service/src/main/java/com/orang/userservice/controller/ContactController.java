package com.orang.userservice.controller;

import com.orang.userservice.dto.ContactResponse;
import com.orang.userservice.service.ContactService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class ContactController {

    private final ContactService contactService;

    @PostMapping("/{userId}/contacts/{contactUserId}")
    public ResponseEntity<ContactResponse> addContact(@PathVariable UUID userId, @PathVariable UUID contactUserId) {
        return ResponseEntity.status(201).body(contactService.addContact(userId, contactUserId));
    }

    @GetMapping("/{userId}/contacts")
    public ResponseEntity<List<ContactResponse>> getContacts(@PathVariable UUID userId) {
        return ResponseEntity.ok(contactService.getContacts(userId));
    }

    @DeleteMapping("/{userId}/contacts/{contactUserId}")
    public ResponseEntity<Void> removeContact(@PathVariable UUID userId, @PathVariable UUID contactUserId) {
        contactService.removeContact(userId, contactUserId);
        return ResponseEntity.noContent().build();
    }
}
