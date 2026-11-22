package org.gmp.service_notifications.kafka;

import jakarta.validation.Validator;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.gmp.service_notifications.entity.Notification;
import org.gmp.service_notifications.entity.NotificationType;
import org.gmp.service_notifications.service.NotificationDelivery;
import org.gmp.service_notifications.service.NotificationStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

@Component
public class NotificationKafkaListener {

    private static final Logger log = LoggerFactory.getLogger(NotificationKafkaListener.class);

    private final JsonMapper json;
    private final Validator validator;
    private final NotificationStore store;
    private final NotificationDelivery delivery;

    public NotificationKafkaListener(JsonMapper json, Validator validator,
                                     NotificationStore store, NotificationDelivery delivery) {
        this.json = json;
        this.validator = validator;
        this.store = store;
        this.delivery = delivery;
    }

    @KafkaListener(topics = "invoice-issued")
    public void invoiceIssued(ConsumerRecord<String, String> record) {
        receive(record, NotificationType.FACTURE_EMISE);
    }

    @KafkaListener(topics = "payment-received")
    public void paymentReceived(ConsumerRecord<String, String> record) {
        receive(record, NotificationType.PAIEMENT_RECU);
    }

    private void receive(ConsumerRecord<String, String> record, NotificationType type) {
        InvoiceNotificationEvent event = readEvent(record.value());
        log.info("Événement reçu topic={} partition={} offset={} invoiceId={}",
                record.topic(), record.partition(), record.offset(), event.invoiceId());

        NotificationStore.Creation creation = store.create(event.clientId(), event.invoiceId(), type);
        Notification notification = creation.notification();
        if (!event.clientId().equals(notification.getClientId())) {
            throw new InvalidNotificationEventException("clientId incohérent avec la notification existante.");
        }
        log.info("{} notificationId={} type={}", creation.created() ? "Notification créée" : "Notification déjà connue",
                notification.getId(), type);
        delivery.deliver(notification.getId());
    }

    private InvoiceNotificationEvent readEvent(String payload) {
        if (payload == null) {
            throw new InvalidNotificationEventException("Événement Kafka vide.");
        }
        try {
            InvoiceNotificationEvent event = json.readValue(payload, InvoiceNotificationEvent.class);
            if (event == null || !validator.validate(event).isEmpty()) {
                throw new InvalidNotificationEventException("Champs requis de l’événement invalides.");
            }
            return event;
        } catch (JacksonException ex) {
            throw new InvalidNotificationEventException("JSON Kafka invalide.");
        }
    }
}
