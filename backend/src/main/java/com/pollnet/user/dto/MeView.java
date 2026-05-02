package com.pollnet.user.dto;

import com.pollnet.user.User;

import java.time.OffsetDateTime;
import java.util.UUID;

public record MeView(
        UUID id,
        String username,
        String email,
        String displayName,
        String bio,
        String avatarUrl,
        int inviteQuota,
        OffsetDateTime createdAt
) {
    public static MeView from(User u) {
        return new MeView(
                u.getId(), u.getUsername(), u.getEmail(),
                u.getDisplayName(), u.getBio(), u.getAvatarUrl(),
                u.getInviteQuota(), u.getCreatedAt()
        );
    }
}
