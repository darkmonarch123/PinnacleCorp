package com.pinnacle.alerts.dto;

import com.pinnacle.entity.Notification;

import java.time.Instant;
import java.util.UUID;

public record NotificationResponse(
    UUID id,
    String message,
    UUID referenceAlertId,
    boolean read,
    Instant createdAt
) {
    public static NotificationResponse from(Notification n) {
        return new NotificationResponse(n.getId(), n.getMessage(), n.getReferenceAlertId(), n.isRead(), n.getCreatedAt());
    }
}
