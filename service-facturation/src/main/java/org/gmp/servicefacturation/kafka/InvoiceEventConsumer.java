package org.gmp.servicefacturation.kafka;

import lombok.RequiredArgsConstructor;
import org.gmp.servicefacturation.config.KafkaConfig;
import org.gmp.servicefacturation.dto.PaymentReceivedEvent;
import org.gmp.servicefacturation.dto.SubscriptionRenewedEvent;
import org.gmp.servicefacturation.service.InvoiceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class InvoiceEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(InvoiceEventConsumer.class);

    private final InvoiceService invoiceService;

    @KafkaListener(
            topics = KafkaConfig.TOPIC_SUBSCRIPTION_RENEWED,
            groupId = "${spring.kafka.consumer.group-id:service-facturation-group}"
    )
    public void consumeSubscriptionRenewed(SubscriptionRenewedEvent event) {
        log.info("Événement reçu sur le topic '{}' : eventId={}, subscriptionId={}",
                KafkaConfig.TOPIC_SUBSCRIPTION_RENEWED, event.getEventId(), event.getSubscriptionId());
        try {
            invoiceService.handleSubscriptionRenewed(event);
        } catch (Exception ex) {
            log.error("Erreur lors du traitement de 'subscription-renewed' : {}", ex.getMessage(), ex);
        }
    }

    @KafkaListener(
            topics = KafkaConfig.TOPIC_PAYMENT_RECEIVED,
            groupId = "${spring.kafka.consumer.group-id:service-facturation-group}"
    )
    public void consumePaymentReceived(PaymentReceivedEvent event) {
        log.info("Événement reçu sur le topic '{}' : eventId={}, invoiceId={}",
                KafkaConfig.TOPIC_PAYMENT_RECEIVED, event.getEventId(), event.getInvoiceId());
        try {
            invoiceService.handlePaymentReceived(event);
        } catch (Exception ex) {
            log.error("Erreur lors du traitement de 'payment-received' : {}", ex.getMessage(), ex);
        }
    }
}
