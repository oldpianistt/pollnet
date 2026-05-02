package com.pollnet.invitation.dto;

import com.pollnet.invitation.Invitation;

import java.time.OffsetDateTime;
import java.util.UUID;

public record InvitationView(
        UUID id,
        String token,
        OffsetDateTime expiresAt,
        OffsetDateTime createdAt
) {
    public static InvitationView from(Invitation inv) {
        return new InvitationView(inv.getId(), inv.getToken(), inv.getExpiresAt(), inv.getCreatedAt());
    }
}
