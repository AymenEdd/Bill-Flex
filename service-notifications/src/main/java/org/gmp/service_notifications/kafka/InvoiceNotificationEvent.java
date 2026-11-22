package org.gmp.service_notifications.kafka;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

@JsonIgnoreProperties(ignoreUnknown = true)
public record InvoiceNotificationEvent(
        String eventId,
        @NotNull @Positive Long invoiceId,
        @NotNull @Positive Long clientId) {
}
