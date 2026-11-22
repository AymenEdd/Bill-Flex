package org.gmp.serviceclients.messaging;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.gmp.serviceclients.config.KafkaConfig;
import org.gmp.serviceclients.service.PaymentService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class InvoiceIssuedConsumer {

    private final PaymentService paymentService;

    @KafkaListener(
            topics = KafkaConfig.TOPIC_INVOICE_ISSUED,
            groupId = "${spring.kafka.consumer.group-id:service-clients-group}"
    )
    public void consumeInvoiceIssued(InvoiceIssuedEvent event) {
        log.info("Événement reçu sur topic '{}' : invoiceId={}, montantTTC={}",
                KafkaConfig.TOPIC_INVOICE_ISSUED, event.invoiceId(), event.montantTTC());
        try {
            paymentService.createPendingFromInvoice(event);
        } catch (Exception ex) {
            log.error("Erreur lors du traitement de 'invoice-issued' : {}", ex.getMessage(), ex);
        }
    }
}
