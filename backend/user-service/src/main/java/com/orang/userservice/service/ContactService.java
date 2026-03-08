package com.orang.userservice.service;

import com.orang.shared.exception.BadRequestException;
import com.orang.shared.exception.ResourceNotFoundException;
import com.orang.userservice.dto.ContactResponse;
import com.orang.userservice.entity.Contact;
import com.orang.userservice.entity.Profile;
import com.orang.userservice.repository.ContactRepository;
import com.orang.userservice.repository.ProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ContactService {

    private final ProfileRepository profileRepository;
    private final ContactRepository contactRepository;
    private final RedisTemplate<String, String> redisTemplate;

    public ContactResponse addContact(UUID userId, UUID contactUserId) {
        if (userId.equals(contactUserId)) {
            throw new BadRequestException("You cannot add yourself as a contact");
        }

        if (contactRepository.existsByUserIdAndContactUserId(userId, contactUserId)) {
            throw new BadRequestException("Contact already exists");
        }

        Profile contactProfile = profileRepository.findById(contactUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Contact user not found"));

        Contact contact = Contact.builder()
                .userId(userId)
                .contactUserId(contactUserId)
                .build();

        Contact savedContact = contactRepository.save(contact);

        return toContactResponse(savedContact);
    }

    public List<ContactResponse> getContacts(UUID userId) {
        List<Contact> contacts = contactRepository.findByUserId(userId);
        return contacts.stream().map(this::toContactResponse).toList();
    }

    @Transactional
    public void removeContact(UUID userId, UUID contactUserId) {
        Contact contact = contactRepository.findByUserIdAndContactUserId(userId, contactUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Contact not found"));
        contactRepository.delete(contact);
    }

    private boolean isUserOnline(UUID userId) {
        String onlineStatus = redisTemplate.opsForValue().get("user:" + userId + ":online");
        return "true".equals(onlineStatus);
    }

    private ContactResponse toContactResponse(Contact contact) {
        Profile contactProfile = profileRepository.findById(contact.getContactUserId())
                .orElseThrow(() -> new ResourceNotFoundException("Contact user not found"));
        return ContactResponse.builder()
                .id(contact.getId())
                .userId(contact.getUserId())
                .contactUserId(contact.getContactUserId())
                .displayName(contactProfile.getDisplayName())
                .avatarUrl(contactProfile.getAvatarUrl())
                .status(contact.getStatus())
                .isOnline(isUserOnline(contact.getContactUserId()))
                .createdAt(contact.getCreatedAt())
                .build();
    }
}
