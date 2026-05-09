package com.orang.userservice.controller;

import com.orang.userservice.dto.BatchUserRequest;
import com.orang.userservice.dto.ProfileResponse;
import com.orang.userservice.dto.UpdateProfileRequest;
import com.orang.userservice.dto.UserSummaryDto;
import com.orang.userservice.service.ProfileService;
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
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProfileControllerTest {

    @Mock
    private ProfileService profileService;

    private ProfileController profileController;

    private static final UUID USER_ID = UUID.fromString("844ec9f6-f781-4f67-aab0-1f33cf9734f7");
    private static final UUID OTHER_USER_ID = UUID.fromString("bc9e3f0b-8c33-4ef0-8c8d-b1d0f3f8f9d2");

    @BeforeEach
    void setUp() {
        profileController = new ProfileController(profileService);
    }

    @Test
    @DisplayName("getProfile returns profile response")
    void getProfileReturnsProfileResponse() {
        ProfileResponse profile = profileResponse();
        when(profileService.getProfileById(USER_ID)).thenReturn(profile);

        ResponseEntity<ProfileResponse> response = profileController.getProfile(USER_ID);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(profile);
    }

    @Test
    @DisplayName("createProfile returns created profile response")
    void createProfileReturnsCreatedProfileResponse() {
        ProfileResponse profile = profileResponse();
        when(profileService.createProfile(USER_ID, "Alice")).thenReturn(profile);

        ResponseEntity<ProfileResponse> response = profileController.createProfile(USER_ID, "Alice");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isEqualTo(profile);
    }

    @Test
    @DisplayName("updateProfile returns updated profile response")
    void updateProfileReturnsUpdatedProfileResponse() {
        ProfileResponse profile = profileResponse();
        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setDisplayName("Alice");
        request.setAvatarUrl("https://example.com/avatar.png");
        request.setBio("Bio");

        when(profileService.updateProfile(USER_ID, request)).thenReturn(profile);

        ResponseEntity<ProfileResponse> response = profileController.updateProfile(USER_ID, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(profile);
    }

    @Test
    @DisplayName("searchProfiles returns matching profiles")
    void searchProfilesReturnsMatchingProfiles() {
        ProfileResponse profile = profileResponse();
        when(profileService.searchProfiles("ali")).thenReturn(List.of(profile));

        ResponseEntity<List<ProfileResponse>> response = profileController.searchProfiles("ali");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsExactly(profile);
    }

    @Test
    @DisplayName("getProfilesByIds returns batch summaries")
    void getProfilesByIdsReturnsBatchSummaries() {
        BatchUserRequest request = BatchUserRequest.builder()
                .userIds(Set.of(USER_ID, OTHER_USER_ID))
                .build();
        UserSummaryDto summary = UserSummaryDto.builder()
                .userId(USER_ID)
                .displayName("Alice")
                .avatarUrl("https://example.com/avatar.png")
                .isOnline(true)
                .build();

        when(profileService.getBatchUserSummaries(request.getUserIds())).thenReturn(Map.of(USER_ID, summary));

        ResponseEntity<Map<UUID, UserSummaryDto>> response = profileController.getProfilesByIds(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsEntry(USER_ID, summary);
    }

    private ProfileResponse profileResponse() {
        return ProfileResponse.builder()
                .userId(USER_ID)
                .displayName("Alice")
                .avatarUrl("https://example.com/avatar.png")
                .bio("Bio")
                .lastSeen(LocalDateTime.of(2026, 5, 4, 10, 0))
                .isOnline(true)
                .build();
    }
}