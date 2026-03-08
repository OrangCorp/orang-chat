package com.orang.userservice.controller;

import com.orang.userservice.dto.ProfileResponse;
import com.orang.userservice.dto.UpdateProfileRequest;
import com.orang.userservice.service.ProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class ProfileController {

    private final ProfileService profileService;

    @GetMapping("/profile/{userId}")
    public ResponseEntity<ProfileResponse> getProfile(@PathVariable UUID userId) {
        return ResponseEntity.ok(profileService.getProfileById(userId));
    }

    @PostMapping("/profile/{userId}")
    public ResponseEntity<ProfileResponse> createProfile(@PathVariable UUID userId, @RequestParam String displayName) {
        ProfileResponse profile = profileService.createProfile(userId, displayName);
        return ResponseEntity.status(HttpStatus.CREATED).body(profile);
    }

    @PutMapping("/profile/{userId}")
    public ResponseEntity<ProfileResponse> updateProfile(
            @PathVariable UUID userId,
            @Valid @RequestBody UpdateProfileRequest profile) {
        ProfileResponse updatedProfile = profileService.updateProfile(userId, profile);
        return ResponseEntity.ok(updatedProfile);
    }

    @GetMapping("/search")
    public ResponseEntity<List<ProfileResponse>> searchProfiles(@RequestParam String query) {
        return ResponseEntity.ok(profileService.searchProfiles(query));
    }

    @PostMapping("/online/{userId}")
    public ResponseEntity<Void> setOnlineStatus(
            @PathVariable UUID userId,
            @RequestParam(defaultValue = "true") boolean status) {
        profileService.setOnlineStatus(userId, status);
        return ResponseEntity.ok().build();
    }

}
