package org.gmp.service_notifications.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.gmp.service_notifications.entity.NotificationType;

public record CreateNotificationRequest(
        @NotNull @Positive Long clientId,
        @NotNull @Positive Long invoiceId,
        @NotNull NotificationType type) {
}
