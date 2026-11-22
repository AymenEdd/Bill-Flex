package org.gmp.service_notifications.dto;

import java.time.Instant;

import org.gmp.service_notifications.entity.NotificationFailure;
import org.gmp.service_notifications.entity.NotificationStatus;
import org.gmp.service_notifications.entity.NotificationType;

public record NotificationResponse(
        Long id,
        Long clientId,
        Long invoiceId,
        NotificationType type,
        NotificationStatus status,
        String recipientEmail,
        String subject,
        Instant createdAt,
        Instant sentAt,
        Instant nextAttemptAt,
        Instant deliveryStartedAt,
        NotificationFailure lastFailure) {
}
