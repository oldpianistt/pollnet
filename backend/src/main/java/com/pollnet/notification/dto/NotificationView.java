package com.pollnet.notification.dto;

import com.fasterxml.jackson.databind.JsonNode;
import com.pollnet.notification.Notification;
import com.pollnet.notification.NotificationType;

import java.time.OffsetDateTime;
import java.util.UUID;

public record NotificationView(
        UUID id,
        NotificationType type,
        JsonNode payload,
        OffsetDateTime createdAt,
        OffsetDateTime readAt
) {
    public static NotificationView from(Notification n) {
        return new NotificationView(n.getId(), n.getType(), n.getPayload(), n.getCreatedAt(), n.getReadAt());
    }
}
