package com.orang.userservice.service;

import com.orang.shared.exception.BadRequestException;
import com.orang.shared.exception.ResourceNotFoundException;
import com.orang.userservice.dto.ProfileResponse;
import com.orang.userservice.dto.UpdateProfileRequest;
import com.orang.userservice.entity.Profile;
import com.orang.userservice.repository.ProfileRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProfileServiceTest {

    @Mock
    ProfileRepository profileRepository;

    @Mock
    PresenceService presenceService;

    @InjectMocks
    ProfileService profileService;

    @Test
    void getProfileById_found() {
        UUID id = UUID.randomUUID();
        Profile p = Profile.builder().userId(id).displayName("Alice").avatarUrl("a").bio("b").lastSeen(LocalDateTime.now()).build();
        when(profileRepository.findById(id)).thenReturn(Optional.of(p));
        when(presenceService.isUserOnline(id.toString())).thenReturn(true);

        ProfileResponse resp = profileService.getProfileById(id);

        assertThat(resp.getDisplayName()).isEqualTo("Alice");
        assertThat(resp.isOnline()).isTrue();
        verify(profileRepository).findById(id);
    }

    @Test
    void getProfileById_notFound() {
        UUID id = UUID.randomUUID();
        when(profileRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> profileService.getProfileById(id))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Profile not found");
    }

    @Test
    void createProfile_alreadyExists_throws() {
        UUID id = UUID.randomUUID();
        when(profileRepository.existsByUserId(id)).thenReturn(true);

        assertThatThrownBy(() -> profileService.createProfile(id, "X"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Profile already exists");
    }

    @Test
    void createProfile_savesAndReturns() {
        UUID id = UUID.randomUUID();
        when(profileRepository.existsByUserId(id)).thenReturn(false);
        Profile saved = Profile.builder().userId(id).displayName("Bob").build();
        when(profileRepository.save(any())).thenReturn(saved);
        when(presenceService.isUserOnline(id.toString())).thenReturn(false);

        ProfileResponse resp = profileService.createProfile(id, "Bob");

        assertThat(resp.getDisplayName()).isEqualTo("Bob");
        assertThat(resp.isOnline()).isFalse();
        verify(profileRepository).save(any());
    }

    @Test
    void createProfileIfNotExists_skipsWhenExists() {
        UUID id = UUID.randomUUID();
        when(profileRepository.existsByUserId(id)).thenReturn(true);

        profileService.createProfileIfNotExists(id, "Z");

        verify(profileRepository, never()).save(any());
    }

    @Test
    void createProfileIfNotExists_createsWhenMissing() {
        UUID id = UUID.randomUUID();
        when(profileRepository.existsByUserId(id)).thenReturn(false);

        profileService.createProfileIfNotExists(id, "Z");

        verify(profileRepository).save(any());
    }

    @Test
    void updateProfile_notFound_throws() {
        UUID id = UUID.randomUUID();
        when(profileRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> profileService.updateProfile(id, new UpdateProfileRequest()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void updateProfile_appliesFields() {
        UUID id = UUID.randomUUID();
        Profile existing = Profile.builder().userId(id).displayName("Old").avatarUrl(null).bio(null).build();
        when(profileRepository.findById(id)).thenReturn(Optional.of(existing));
        when(profileRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(presenceService.isUserOnline(id.toString())).thenReturn(true);

        UpdateProfileRequest req = new UpdateProfileRequest();
        req.setDisplayName("New");
        req.setAvatarUrl("u");
        req.setBio("bio");

        ProfileResponse resp = profileService.updateProfile(id, req);

        assertThat(resp.getDisplayName()).isEqualTo("New");
        assertThat(resp.getAvatarUrl()).isEqualTo("u");
        assertThat(resp.getBio()).isEqualTo("bio");
    }

    @Test
    void getBatchUserSummaries_returnsMap() {
        UUID a = UUID.randomUUID();
        UUID b = UUID.randomUUID();
        Profile p1 = Profile.builder().userId(a).displayName("A").build();
        Profile p2 = Profile.builder().userId(b).displayName("B").build();
        when(profileRepository.findAllById(Set.of(a,b))).thenReturn(List.of(p1,p2));
        when(presenceService.isUserOnline(anyString())).thenReturn(false);

        var map = profileService.getBatchUserSummaries(Set.of(a,b));
        assertThat(map).containsKeys(a,b);
    }

    @Test
    void searchProfiles_mapsToResponses() {
        UUID a = UUID.randomUUID();
        Profile p = Profile.builder().userId(a).displayName("SearchMe").build();
        when(profileRepository.findByDisplayNameContainingIgnoreCase("me")).thenReturn(List.of(p));
        when(presenceService.isUserOnline(anyString())).thenReturn(false);

        var list = profileService.searchProfiles("me");
        assertThat(list).hasSize(1);
        assertThat(list.get(0).getDisplayName()).isEqualTo("SearchMe");
    }
}
