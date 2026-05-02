package com.pollnet.user.dto;

import com.pollnet.user.User;

import java.time.OffsetDateTime;
import java.util.UUID;

public record UserSummary(
        UUID id,
        String username,
        String displayName,
        String avatarUrl,
        OffsetDateTime lastSeenAt
) {
    public static UserSummary from(User u) {
        return new UserSummary(u.getId(), u.getUsername(), u.getDisplayName(), u.getAvatarUrl(), u.getLastSeenAt());
    }
}
