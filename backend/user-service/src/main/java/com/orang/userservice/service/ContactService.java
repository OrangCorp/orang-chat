package com.orang.userservice.service;

import com.orang.userservice.dto.ContactResponse;
import com.orang.userservice.entity.Contact;
import com.orang.userservice.entity.Profile;
import com.orang.userservice.repository.ContactRepository;
import com.orang.userservice.repository.ProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ContactService {

    private final ProfileRepository profileRepository;
    private final ContactRepository contactRepository;
    private final RedisTemplate<String, String> redisTemplate;

    public ContactResponse addContact(UUID userId, UUID contactUserId) {
        if (userId.equals(contactUserId)) {
            throw new RuntimeException("You cannot add yourself as a contact");
        }

        if (contactRepository.existsByUserIdAndContactUserId(userId, contactUserId)) {
            throw new RuntimeException("Contact already exists");
        }

        Profile contactProfile = profileRepository.findById(contactUserId)
                .orElseThrow(() -> new RuntimeException("Contact user not found"));

        Contact contact = Contact.builder()
                .userId(userId)
                .contactUserId(contactUserId)
                .build();

        Contact savedContact = contactRepository.save(contact);

        return ContactResponse.builder()
                .id(savedContact.getId())
                .userId(savedContact.getUserId())
                .contactUserId(savedContact.getContactUserId())
                .displayName(contactProfile.getDisplayName())
                .avatarUrl(contactProfile.getAvatarUrl())
                .status(savedContact.getStatus())
                .isOnline(isUserOnline(contactUserId))
                .createdAt(savedContact.getCreatedAt())
                .build();
    }

    private boolean isUserOnline(UUID userId) {
        String onlineStatus = redisTemplate.opsForValue().get("user:" + userId + ":online");
        return "true".equals(onlineStatus);
    }
}
