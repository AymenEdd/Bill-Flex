package org.gmp.serviceabonnements.kafka;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import java.time.Instant;
import java.time.LocalDate;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "eventType")
@JsonSubTypes({
        @JsonSubTypes.Type(value = SubscriptionEvent.SubscriptionCreatedEvent.class, name = "SUBSCRIPTION_CREATED"),
        @JsonSubTypes.Type(value = SubscriptionEvent.SubscriptionRenewedEvent.class, name = "SUBSCRIPTION_RENEWED"),
        @JsonSubTypes.Type(value = SubscriptionEvent.SubscriptionExpiredEvent.class, name = "SUBSCRIPTION_EXPIRED"),
        @JsonSubTypes.Type(value = SubscriptionEvent.SubscriptionCancelledEvent.class, name = "SUBSCRIPTION_CANCELLED")
})
public sealed interface SubscriptionEvent permits SubscriptionEvent.SubscriptionCreatedEvent, SubscriptionEvent.SubscriptionRenewedEvent, SubscriptionEvent.SubscriptionExpiredEvent, SubscriptionEvent.SubscriptionCancelledEvent {

    String eventId();
    Instant timestamp();
    Long subscriptionId();
    Long clientId();
    String planName();
    String eventType();

    record SubscriptionCreatedEvent(
            String eventId,
            Instant timestamp,
            Long subscriptionId,
            Long clientId,
            String planName,
            LocalDate dateDebut,
            LocalDate dateFin
    ) implements SubscriptionEvent {
        @Override
        public String eventType() {
            return "SUBSCRIPTION_CREATED";
        }
    }

    record SubscriptionRenewedEvent(
            String eventId,
            Instant timestamp,
            Long subscriptionId,
            Long clientId,
            Long planId,
            String planName,
            String periode,
            LocalDate dateDebut,
            LocalDate dateFin
    ) implements SubscriptionEvent {
        @Override
        public String eventType() {
            return "SUBSCRIPTION_RENEWED";
        }
    }

    record SubscriptionExpiredEvent(
            String eventId,
            Instant timestamp,
            Long subscriptionId,
            Long clientId,
            String planName,
            LocalDate dateFin
    ) implements SubscriptionEvent {
        @Override
        public String eventType() {
            return "SUBSCRIPTION_EXPIRED";
        }
    }

    record SubscriptionCancelledEvent(
            String eventId,
            Instant timestamp,
            Long subscriptionId,
            Long clientId,
            String planName,
            LocalDate dateFin
    ) implements SubscriptionEvent {
        @Override
        public String eventType() {
            return "SUBSCRIPTION_CANCELLED";
        }
    }
}