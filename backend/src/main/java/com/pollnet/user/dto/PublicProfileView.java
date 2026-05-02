package com.pollnet.user.dto;

import com.pollnet.user.User;

import java.time.OffsetDateTime;
import java.util.UUID;

public record PublicProfileView(
        UUID id,
        String username,
        String displayName,
        String bio,
        String avatarUrl,
        OffsetDateTime createdAt,
        OffsetDateTime lastSeenAt,
        long followerCount,
        long followingCount,
        boolean viewerFollows
) {
    public static PublicProfileView from(User u, long followers, long following, boolean viewerFollows) {
        return new PublicProfileView(
                u.getId(), u.getUsername(), u.getDisplayName(), u.getBio(),
                u.getAvatarUrl(), u.getCreatedAt(), u.getLastSeenAt(),
                followers, following, viewerFollows
        );
    }
}
