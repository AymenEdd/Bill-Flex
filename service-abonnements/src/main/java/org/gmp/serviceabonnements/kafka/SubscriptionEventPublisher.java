package org.gmp.serviceabonnements.kafka;

import org.gmp.serviceabonnements.config.KafkaConfig;
import org.gmp.serviceabonnements.entity.Abonnement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

@Component
public class SubscriptionEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(SubscriptionEventPublisher.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public SubscriptionEventPublisher(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publishCreated(Abonnement abonnement, String planName) {
        var event = new SubscriptionEvent.SubscriptionCreatedEvent(
                UUID.randomUUID().toString(),
                Instant.now(),
                abonnement.getId(),
                abonnement.getClientId(),
                planName,
                abonnement.getDateDebut(),
                abonnement.getDateFin()
        );
        send(KafkaConfig.TOPIC_SUBSCRIPTION_CREATED, event);
    }

    public void publishRenewed(Abonnement abonnement, String planName) {
        Long planId = (abonnement.getPlan() != null) ? abonnement.getPlan().getId() : null;
        String periode = (abonnement.getProchaineDateFacturation() != null)
                ? abonnement.getProchaineDateFacturation().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM"))
                : java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM"));
        publishRenewed(abonnement, planName, planId, periode);
    }

    public void publishRenewed(Abonnement abonnement, String planName, Long planId, String periode) {
        var event = new SubscriptionEvent.SubscriptionRenewedEvent(
                UUID.randomUUID().toString(),
                Instant.now(),
                abonnement.getId(),
                abonnement.getClientId(),
                planId,
                planName,
                periode,
                abonnement.getDateDebut(),
                abonnement.getDateFin()
        );
        send(KafkaConfig.TOPIC_SUBSCRIPTION_RENEWED, event);
    }

    public void publishExpired(Abonnement abonnement, String planName) {
        var event = new SubscriptionEvent.SubscriptionExpiredEvent(
                UUID.randomUUID().toString(),
                Instant.now(),
                abonnement.getId(),
                abonnement.getClientId(),
                planName,
                abonnement.getDateFin()
        );
        send(KafkaConfig.TOPIC_SUBSCRIPTION_EXPIRED, event);
    }

    public void publishCancelled(Abonnement abonnement, String planName) {
        var event = new SubscriptionEvent.SubscriptionCancelledEvent(
                UUID.randomUUID().toString(),
                Instant.now(),
                abonnement.getId(),
                abonnement.getClientId(),
                planName,
                abonnement.getDateFin()
        );
        send(KafkaConfig.TOPIC_SUBSCRIPTION_CANCELLED, event);
    }

    private void send(String topic, SubscriptionEvent event) {
        kafkaTemplate.send(topic, String.valueOf(event.subscriptionId()), event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to send {} event to topic {}: {}", event.eventType(), topic, ex.getMessage());
                    } else {
                        log.info("Published {} event to topic {} (partition={}, offset={})",
                                event.eventType(), topic, result.getRecordMetadata().partition(), result.getRecordMetadata().offset());
                    }
                });
    }
}