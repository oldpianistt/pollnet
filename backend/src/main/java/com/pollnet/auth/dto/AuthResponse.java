package com.pollnet.auth.dto;

import java.util.UUID;

public record AuthResponse(
        String accessToken,
        String refreshToken,
        long accessTtlSeconds,
        UserSummary user
) {
    public record UserSummary(
            UUID id, String username, String email,
            String displayName, String avatarUrl
    ) {}
}
