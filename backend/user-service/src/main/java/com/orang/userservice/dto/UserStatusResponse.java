package com.orang.userservice.dto;

import com.orang.shared.presence.UserStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserStatusResponse {

    private String userId;
    private UserStatus status;

    public static UserStatusResponse from(String userId ,UserStatus status) {
        return UserStatusResponse.builder()
                .userId(userId)
                .status(status)
                .build();
    }
}
