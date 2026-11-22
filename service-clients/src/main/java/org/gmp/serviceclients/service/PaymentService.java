package org.gmp.serviceclients.service;

import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.gmp.serviceclients.client.FacturationClient;
import org.gmp.serviceclients.client.InvoiceDto;
import org.gmp.serviceclients.config.KafkaConfig;
import org.gmp.serviceclients.dto.PaymentRequest;
import org.gmp.serviceclients.dto.PaymentResponse;
import org.gmp.serviceclients.enums.ModePaiement;
import org.gmp.serviceclients.enums.StatutPaiement;
import org.gmp.serviceclients.exception.DuplicateResourceException;
import org.gmp.serviceclients.exception.ResourceNotFoundException;
import org.gmp.serviceclients.messaging.InvoiceIssuedEvent;
import org.gmp.serviceclients.messaging.PaymentReceivedEvent;
import org.gmp.serviceclients.models.Client;
import org.gmp.serviceclients.models.Payment;
import org.gmp.serviceclients.repositories.Clientrepository;
import org.gmp.serviceclients.repositories.PaymentRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final Clientrepository clientRepository;
    private final FacturationClient facturationClient;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    /** V4 : création manuelle. La facture doit exister (Feign) et ne pas avoir déjà de paiement. */
    public PaymentResponse create(PaymentRequest dto) {
        InvoiceDto invoice = fetchInvoice(dto.invoiceId());

        if (paymentRepository.existsByInvoiceId(dto.invoiceId())) {
            throw new DuplicateResourceException(
                    "Un paiement existe déjà pour la facture " + dto.invoiceId());
        }

        Payment payment = Payment.builder()
                .invoiceId(invoice.id())
                .clientId(invoice.clientId())
                .montant(dto.montant() != null ? dto.montant() : invoice.montantTTC())
                .moyen(dto.moyen() != null ? dto.moyen() : defaultMoyen(invoice.clientId()))
                .statut(StatutPaiement.EN_ATTENTE)
                .build();

        return toResponse(paymentRepository.saveAndFlush(payment));
    }

    /** V5 : consommateur de invoice-issued. Idempotent : rien si un paiement existe déjà. */
    public void createPendingFromInvoice(InvoiceIssuedEvent event) {
        if (paymentRepository.existsByInvoiceId(event.invoiceId())) {
            log.info("invoice-issued ignoré : paiement déjà existant pour la facture {}", event.invoiceId());
            return;
        }

        Payment payment = Payment.builder()
                .invoiceId(event.invoiceId())
                .clientId(event.clientId())
                .montant(event.montantTTC())
                .moyen(defaultMoyen(event.clientId()))
                .statut(StatutPaiement.EN_ATTENTE)
                .build();

        try {
            paymentRepository.saveAndFlush(payment);
            log.info("Paiement EN_ATTENTE créé pour la facture {}", event.invoiceId());
        } catch (DataIntegrityViolationException e) {
            log.info("invoice-issued ignoré (doublon concurrent) pour la facture {}", event.invoiceId());
        }
    }

    /** V5 : EN_ATTENTE -> PAYE, puis publication de payment-received. */
    public PaymentResponse pay(Long id) {
        Payment payment = paymentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Paiement", id));

        if (payment.getStatut() == StatutPaiement.PAYE) {
            return toResponse(payment);
        }

        payment.setStatut(StatutPaiement.PAYE);
        payment.setDate(LocalDate.now());
        Payment saved = paymentRepository.save(payment);

        PaymentReceivedEvent event = new PaymentReceivedEvent(
                saved.getId(),
                saved.getInvoiceId(),
                saved.getClientId(),
                saved.getMontant(),
                saved.getDate(),
                saved.getMoyen() != null ? saved.getMoyen().name() : null
        );

        try {
            kafkaTemplate.send(KafkaConfig.TOPIC_PAYMENT_RECEIVED, String.valueOf(saved.getId()), event);
            log.info("Événement 'payment-received' publié pour le paiement id={}", saved.getId());
        } catch (Exception e) {
            log.warn("Kafka inaccessible : l'événement n'a pas pu être publié immédiatement ({})", e.getMessage());
        }

        return toResponse(saved);
    }

    public PaymentResponse findById(Long id) {
        return paymentRepository.findById(id)
                .map(this::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Paiement", id));
    }

    public List<PaymentResponse> findAll(Long invoiceId) {
        List<Payment> payments = invoiceId != null
                ? paymentRepository.findByInvoiceId(invoiceId)
                : paymentRepository.findAll();
        return payments.stream().map(this::toResponse).toList();
    }

    private InvoiceDto fetchInvoice(Long invoiceId) {
        try {
            return facturationClient.getInvoiceById(invoiceId);
        } catch (FeignException.NotFound e) {
            throw new ResourceNotFoundException("Facture", invoiceId);
        }
    }

    private ModePaiement defaultMoyen(Long clientId) {
        return clientRepository.findById(clientId)
                .map(Client::getModePaiement)
                .orElse(null);
    }

    private PaymentResponse toResponse(Payment p) {
        return new PaymentResponse(p.getId(), p.getInvoiceId(), p.getClientId(),
                p.getDate(), p.getMontant(), p.getMoyen(), p.getStatut());
    }
}
