package com.orang.userservice.service;

import com.orang.shared.exception.BadRequestException;
import com.orang.shared.exception.ResourceNotFoundException;
import com.orang.userservice.dto.ProfileResponse;
import com.orang.userservice.dto.UpdateProfileRequest;
import com.orang.userservice.entity.Profile;
import com.orang.userservice.repository.ProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProfileService {

    private final ProfileRepository profileRepository;
    private final RedisTemplate<String, String> redisTemplate;

    public ProfileResponse getProfileById(UUID userId) {
        Profile profile = profileRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Profile not found"));
        return toProfileResponse(profile);
    }

    // For Rest API
    @Transactional
    public ProfileResponse createProfile(UUID userId, String displayName) {
        if (profileRepository.existsByUserId(userId)) {
            throw new BadRequestException("Profile already exists");
        }

        Profile profile = Profile.builder()
                .userId(userId)
                .displayName(displayName)
                .build();

        Profile savedProfile = profileRepository.save(profile);
        return toProfileResponse(savedProfile);
    }

    // For event listeners - idempotent
    @Transactional
    public void createProfileIfNotExists(UUID userId, String displayName) {
        if (profileRepository.existsByUserId(userId)) {
            log.debug("Profile already exists for userId={}, skipping", userId);
            return;
        }

        Profile profile = Profile.builder()
                .userId(userId)
                .displayName(displayName)
                .build();

        profileRepository.save(profile);
        log.info("Profile created for userId={}", userId);
    }

    @Transactional
    public ProfileResponse updateProfile(UUID userId, UpdateProfileRequest request) {
        Profile existingProfile = profileRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Profile not found"));

        if (request.getDisplayName() != null) {
            existingProfile.setDisplayName(request.getDisplayName());
        }
        if (request.getAvatarUrl() != null) {
            existingProfile.setAvatarUrl(request.getAvatarUrl());
        }
        if (request.getBio() != null) {
            existingProfile.setBio(request.getBio());
        }

        Profile updatedProfile = profileRepository.save(existingProfile);
        return toProfileResponse(updatedProfile);
    }

    public List<ProfileResponse> searchProfiles(String query) {
        return profileRepository.findByDisplayNameContainingIgnoreCase(query).stream()
                .map(this::toProfileResponse)
                .toList();
    }

    private ProfileResponse toProfileResponse(Profile profile) {
        return ProfileResponse.builder()
                .userId(profile.getUserId())
                .displayName(profile.getDisplayName())
                .avatarUrl(profile.getAvatarUrl())
                .bio(profile.getBio())
                .lastSeen(profile.getLastSeen())
                .isOnline(isUserOnline(profile.getUserId()))
                .build();
    }

    private boolean isUserOnline(UUID userId) {
        String onlineStatus = redisTemplate.opsForValue().get("user:" + userId + ":online");
        return "true".equals(onlineStatus);
    }

    public void setOnlineStatus(UUID userId, boolean online) {
        String key = "user:" + userId + ":online";

        if (online) {
            redisTemplate.opsForValue().set(key, "true");
        } else {
            redisTemplate.delete(key);
        }
    }
}
