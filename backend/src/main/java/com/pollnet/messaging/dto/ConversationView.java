package com.pollnet.messaging.dto;

import com.pollnet.user.dto.UserSummary;

import java.time.OffsetDateTime;
import java.util.UUID;

public record ConversationView(
        UUID id,
        UserSummary peer,
        String lastMessagePreview,   // null until first message; decrypted by service
        OffsetDateTime lastMessageAt,
        long unreadCount
) {}
