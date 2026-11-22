package org.gmp.servicefacturation.kafka;

import lombok.RequiredArgsConstructor;
import org.gmp.servicefacturation.config.KafkaConfig;
import org.gmp.servicefacturation.dto.InvoiceIssuedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class InvoiceEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(InvoiceEventPublisher.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void publishInvoiceIssued(InvoiceIssuedEvent event) {
        log.info("Publication de l'événement Kafka 'invoice-issued' pour la facture id={} (periode={})",
                event.getInvoiceId(), event.getPeriode());
        try {
            kafkaTemplate.send(KafkaConfig.TOPIC_INVOICE_ISSUED, String.valueOf(event.getInvoiceId()), event);
        } catch (Exception ex) {
            log.error("Erreur lors de l'envoi du message Kafka 'invoice-issued' : {}", ex.getMessage());
        }
    }
}
