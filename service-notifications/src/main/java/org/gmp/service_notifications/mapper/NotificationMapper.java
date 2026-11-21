package org.gmp.service_notifications.mapper;

import org.gmp.service_notifications.dto.NotificationResponse;
import org.gmp.service_notifications.entity.Notification;

public final class NotificationMapper {

    private NotificationMapper() {
    }

    public static NotificationResponse toResponse(Notification notification) {
        return new NotificationResponse(
                notification.getId(), notification.getClientId(), notification.getInvoiceId(),
                notification.getType(), notification.getStatus(), notification.getRecipientEmail(),
                notification.getSubject(), notification.getCreatedAt(), notification.getSentAt(),
                notification.getNextAttemptAt(), notification.getDeliveryStartedAt(),
                notification.getLastFailure());
    }
}
