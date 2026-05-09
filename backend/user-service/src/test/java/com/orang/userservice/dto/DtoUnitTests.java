package com.orang.userservice.dto;

import com.orang.shared.presence.UserStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("User-service DTO unit tests")
class DtoUnitTests {

    @Test
    void sessionInfo_from_withValidTimestamps() {
        Map<Object, Object> meta = new HashMap<>();
        meta.put("connectedAt", String.valueOf(Instant.parse("2026-01-01T00:00:00Z").getEpochSecond()));
        meta.put("lastActiveAt", String.valueOf(Instant.parse("2026-01-01T00:10:00Z").getEpochSecond()));
        meta.put("userAgent", "JUnit-Agent");

        SessionInfoResponse r = SessionInfoResponse.from("s1", meta);
        assertThat(r.getSessionId()).isEqualTo("s1");
        assertThat(r.getUserAgent()).isEqualTo("JUnit-Agent");
        assertThat(r.getConnectedAt()).isNotNull();
        assertThat(r.getLastActiveAt()).isNotNull();
    }

    @Test
    void sessionInfo_from_withInvalidTimestamp() {
        Map<Object, Object> meta = new HashMap<>();
        meta.put("connectedAt", "not-a-number");

        SessionInfoResponse r = SessionInfoResponse.from("s2", meta);
        assertThat(r.getConnectedAt()).isNull();
    }

    @Test
    void lastSeen_from_nullAndValue() {
        LastSeenResponse r1 = LastSeenResponse.from("u1", null, false);
        assertThat(r1.getLastSeenAt()).isNull();
        assertThat(r1.isOnline()).isFalse();

        Long ts = Instant.parse("2026-01-02T12:00:00Z").getEpochSecond();
        LastSeenResponse r2 = LastSeenResponse.from("u2", ts, true);
        assertThat(r2.getLastSeenAt()).isNotNull();
        assertThat(r2.isOnline()).isTrue();
    }

    @Test
    void userStatus_from_and_contact_response_builder() {
        UserStatusResponse s = UserStatusResponse.from("u1", UserStatus.ONLINE);
        assertThat(s.getUserId()).isEqualTo("u1");
        assertThat(s.getStatus()).isEqualTo(UserStatus.ONLINE);

        ContactResponse c = ContactResponse.builder()
                .id(UUID.randomUUID())
                .requesterId(UUID.randomUUID())
                .recipientId(UUID.randomUUID())
                .status(com.orang.userservice.entity.ContactStatus.ACCEPTED)
                .build();

        assertThat(c.getStatus()).isEqualTo(com.orang.userservice.entity.ContactStatus.ACCEPTED);
    }

    @Test
    void profile_and_summary_and_batch_requests() {
        ProfileResponse p = ProfileResponse.builder()
                .userId(UUID.randomUUID())
                .displayName("Test User")
                .avatarUrl("http://example.com/a.png")
                .bio("hello")
                .isOnline(true)
                .build();

        assertThat(p.getDisplayName()).isEqualTo("Test User");

        UserSummaryDto us = UserSummaryDto.builder()
                .userId(UUID.randomUUID())
                .displayName("Summary")
                .isOnline(false)
                .build();

        assertThat(us.isOnline()).isFalse();

        BatchUserRequest bu = BatchUserRequest.builder().userIds(Set.of(UUID.randomUUID())).build();
        assertThat(bu.getUserIds()).hasSize(1);

        BatchStatusRequest bs = new BatchStatusRequest(List.of("u1","u2"));
        assertThat(bs.getUserIds()).containsExactly("u1","u2");

        UpdateProfileRequest up = new UpdateProfileRequest();
        up.setDisplayName("X");
        up.setAvatarUrl("a");
        up.setBio("b");
        assertThat(up.getAvatarUrl()).isEqualTo("a");
    }
}
