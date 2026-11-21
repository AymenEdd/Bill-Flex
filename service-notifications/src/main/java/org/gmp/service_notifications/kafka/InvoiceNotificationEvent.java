package org.gmp.service_notifications.kafka;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

record InvoiceNotificationEvent(
        @NotBlank @Size(max = 128) String eventId,
        @NotNull @Positive Long invoiceId,
        @NotNull @Positive Long clientId) {
}
