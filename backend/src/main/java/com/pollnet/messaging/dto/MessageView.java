package com.pollnet.messaging.dto;

import com.pollnet.user.dto.UserSummary;

import java.time.OffsetDateTime;
import java.util.UUID;

public record MessageView(
        UUID id,
        UserSummary sender,
        String body,
        String attachmentUrl,
        OffsetDateTime createdAt,
        OffsetDateTime readAt
) {}
