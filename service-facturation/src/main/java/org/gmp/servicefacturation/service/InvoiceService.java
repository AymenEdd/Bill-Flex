package org.gmp.servicefacturation.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.gmp.servicefacturation.client.ExternalServicesClient;
import org.gmp.servicefacturation.dto.*;
import org.gmp.servicefacturation.entity.Invoice;
import org.gmp.servicefacturation.entity.InvoiceLine;
import org.gmp.servicefacturation.entity.StatutFacture;
import org.gmp.servicefacturation.exception.BusinessValidationException;
import org.gmp.servicefacturation.exception.IdempotencyConflictException;
import org.gmp.servicefacturation.exception.IllegalStateTransitionException;
import org.gmp.servicefacturation.exception.ResourceNotFoundException;
import org.gmp.servicefacturation.kafka.InvoiceEventPublisher;
import org.gmp.servicefacturation.mapper.InvoiceMapper;
import org.gmp.servicefacturation.repository.InvoiceLineRepository;
import org.gmp.servicefacturation.repository.InvoiceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class InvoiceService {

    private static final Logger log = LoggerFactory.getLogger(InvoiceService.class);

    private final InvoiceRepository invoiceRepository;
    private final InvoiceLineRepository invoiceLineRepository;
    private final InvoiceMapper invoiceMapper;
    private final ExternalServicesClient externalServicesClient;
    private final InvoiceEventPublisher invoiceEventPublisher;

    public InvoiceResponse createInvoice(InvoiceRequest request) {
        log.info("Création d'une nouvelle facture pour subscriptionId={} et periode={}",
                request.getSubscriptionId(), request.getPeriode());

        if (invoiceRepository.existsBySubscriptionIdAndPeriode(request.getSubscriptionId(), request.getPeriode())) {
            throw new IdempotencyConflictException(
                    "Une facture existe déjà pour l'abonnement " + request.getSubscriptionId()
                            + " et la période " + request.getPeriode()
            );
        }

        Invoice invoice = invoiceMapper.toEntity(request);
        Invoice saved = invoiceRepository.save(invoice);
        return invoiceMapper.toResponse(saved);
    }

    public List<InvoiceResponse> getAllInvoices(Long clientId, Long subscriptionId, StatutFacture statut) {
        if (clientId != null) {
            return invoiceRepository.findByClientId(clientId).stream().map(invoiceMapper::toResponse).toList();
        }
        if (subscriptionId != null) {
            return invoiceRepository.findBySubscriptionId(subscriptionId).stream().map(invoiceMapper::toResponse).toList();
        }
        if (statut != null) {
            return invoiceRepository.findByStatut(statut).stream().map(invoiceMapper::toResponse).toList();
        }
        return invoiceRepository.findAll().stream().map(invoiceMapper::toResponse).toList();
    }

    public InvoiceResponse getInvoiceById(Long id) {
        Invoice invoice = findEntityById(id);
        return invoiceMapper.toResponse(invoice);
    }

    public InvoiceEnrichedResponse getInvoiceEnrichedDetails(Long id) {
        log.info("Consultation enrichie de la facture id={}", id);
        Invoice invoice = findEntityById(id);
        InvoiceResponse invoiceResponse = invoiceMapper.toResponse(invoice);

        ClientDto clientDto = null;
        try {
            clientDto = externalServicesClient.getClient(invoice.getClientId());
        } catch (Exception e) {
            log.warn("Impossible de récupérer le client id={} : {}", invoice.getClientId(), e.getMessage());
        }

        AbonnementDto abonnementDto = null;
        PlanDto planDto = null;
        try {
            abonnementDto = externalServicesClient.getAbonnement(invoice.getSubscriptionId());
            if (abonnementDto != null && abonnementDto.getPlanId() != null) {
                planDto = externalServicesClient.getPlan(abonnementDto.getPlanId());
            }
        } catch (Exception e) {
            log.warn("Impossible de récupérer l'abonnement id={} : {}", invoice.getSubscriptionId(), e.getMessage());
        }

        return InvoiceEnrichedResponse.builder()
                .invoice(invoiceResponse)
                .client(clientDto)
                .abonnement(abonnementDto)
                .plan(planDto)
                .build();
    }

    public InvoiceResponse updateInvoice(Long id, InvoiceRequest request) {
        Invoice invoice = findEntityById(id);

        if (invoice.getStatut() != StatutFacture.BROUILLON) {
            throw new IllegalStateTransitionException(
                    "Transition interdite : Seule une facture en statut BROUILLON peut être modifiée (statut actuel : "
                            + invoice.getStatut() + ")"
            );
        }

        invoice.setPeriode(request.getPeriode());
        invoice.setEcheance(request.getEcheance());
        if (request.getTauxTva() != null) {
            invoice.setTauxTva(request.getTauxTva());
        }
        invoice.recalculerMontants();

        Invoice updated = invoiceRepository.save(invoice);
        return invoiceMapper.toResponse(updated);
    }

    public void deleteInvoice(Long id) {
        Invoice invoice = findEntityById(id);
        if (invoice.getStatut() != StatutFacture.BROUILLON) {
            throw new IllegalStateTransitionException(
                    "Transition interdite : Seule une facture en statut BROUILLON peut être supprimée (statut actuel : "
                            + invoice.getStatut() + ")"
            );
        }
        invoiceRepository.delete(invoice);
    }

    public InvoiceResponse addLineToInvoice(Long invoiceId, InvoiceLineRequest lineRequest) {
        Invoice invoice = findEntityById(invoiceId);

        if (invoice.getStatut() != StatutFacture.BROUILLON) {
            throw new IllegalStateTransitionException(
                    "Transition interdite : Impossible d'ajouter une ligne à une facture qui n'est pas en statut BROUILLON"
            );
        }

        InvoiceLine line = invoiceMapper.toEntity(lineRequest);
        invoice.addLine(line);
        invoice.recalculerMontants();

        Invoice saved = invoiceRepository.save(invoice);
        return invoiceMapper.toResponse(saved);
    }

    public InvoiceResponse removeLineFromInvoice(Long invoiceId, Long lineId) {
        Invoice invoice = findEntityById(invoiceId);

        if (invoice.getStatut() != StatutFacture.BROUILLON) {
            throw new IllegalStateTransitionException(
                    "Transition interdite : Impossible de modifier les lignes d'une facture qui n'est pas en statut BROUILLON"
            );
        }

        InvoiceLine lineToRemove = invoice.getLines().stream()
                .filter(l -> l.getId() != null && l.getId().equals(lineId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Ligne de facture introuvable avec l'id : " + lineId));

        invoice.removeLine(lineToRemove);
        invoice.recalculerMontants();

        Invoice saved = invoiceRepository.save(invoice);
        return invoiceMapper.toResponse(saved);
    }

    public InvoiceResponse emitInvoice(Long id) {
        Invoice invoice = findEntityById(id);

        if (invoice.getStatut() != StatutFacture.BROUILLON) {
            throw new IllegalStateTransitionException(
                    "Transition interdite : Seule une facture en statut BROUILLON peut être émise (statut actuel : "
                            + invoice.getStatut() + ")"
            );
        }

        log.info("Validation Feign avant émission pour invoice id={}", id);
        AbonnementDto abonnement = externalServicesClient.getAbonnement(invoice.getSubscriptionId());
        if (abonnement == null || abonnement.getStatut() == null ||
                (!abonnement.getStatut().equalsIgnoreCase("ACTIF") && !abonnement.getStatut().equalsIgnoreCase("ACTIVE"))) {
            throw new BusinessValidationException(
                    "Émission impossible : L'abonnement id=" + invoice.getSubscriptionId()
                            + " doit être actif pour émettre la facture (statut actuel: "
                            + (abonnement != null ? abonnement.getStatut() : "null") + ")"
            );
        }

        ClientDto client = externalServicesClient.getClient(invoice.getClientId());
        if (client == null) {
            throw new BusinessValidationException("Émission impossible : Le client id=" + invoice.getClientId() + " est introuvable");
        }
        if (client.getNumeroTVA() == null || client.getNumeroTVA().trim().isEmpty()) {
            throw new BusinessValidationException("Émission impossible : Le client doit avoir un numéro de TVA valide pour émettre la facture");
        }
        if (client.getModePaiement() == null || client.getModePaiement().trim().isEmpty()) {
            throw new BusinessValidationException("Émission impossible : Le client doit avoir un mode de paiement configuré pour émettre la facture");
        }

        invoice.setStatut(StatutFacture.EMISE);
        Invoice saved = invoiceRepository.save(invoice);

        InvoiceIssuedEvent event = InvoiceIssuedEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .invoiceId(saved.getId())
                .subscriptionId(saved.getSubscriptionId())
                .clientId(saved.getClientId())
                .periode(saved.getPeriode())
                .montantHT(saved.getMontantHT())
                .montantTTC(saved.getMontantTTC())
                .echeance(saved.getEcheance())
                .dateEmission(LocalDateTime.now())
                .build();

        invoiceEventPublisher.publishInvoiceIssued(event);

        return invoiceMapper.toResponse(saved);
    }

    public InvoiceResponse markAsPaid(Long invoiceId) {
        Invoice invoice = findEntityById(invoiceId);

        if (invoice.getStatut() != StatutFacture.EMISE && invoice.getStatut() != StatutFacture.EN_RETARD) {
            throw new IllegalStateTransitionException(
                    "Transition interdite : Seule une facture au statut EMISE ou EN_RETARD peut être marquée PAYEE (statut actuel : "
                            + invoice.getStatut() + ")"
            );
        }

        invoice.setStatut(StatutFacture.PAYEE);
        Invoice saved = invoiceRepository.save(invoice);
        log.info("Facture id={} marquée comme PAYEE avec succès", invoiceId);
        return invoiceMapper.toResponse(saved);
    }

    public InvoiceResponse markAsOverdue(Long invoiceId) {
        Invoice invoice = findEntityById(invoiceId);

        if (invoice.getStatut() != StatutFacture.EMISE) {
            throw new IllegalStateTransitionException(
                    "Transition interdite : Seule une facture au statut EMISE peut passer à EN_RETARD (statut actuel : "
                            + invoice.getStatut() + ")"
            );
        }

        invoice.setStatut(StatutFacture.EN_RETARD);
        Invoice saved = invoiceRepository.save(invoice);
        log.info("Facture id={} marquée comme EN_RETARD", invoiceId);
        return invoiceMapper.toResponse(saved);
    }

    public InvoiceResponse cancelInvoice(Long invoiceId) {
        Invoice invoice = findEntityById(invoiceId);

        if (invoice.getStatut() == StatutFacture.PAYEE || invoice.getStatut() == StatutFacture.ANNULEE) {
            throw new IllegalStateTransitionException(
                    "Transition interdite : Une facture déjà PAYEE ou déjà ANNULEE ne peut pas être annulée (statut actuel : "
                            + invoice.getStatut() + ")"
            );
        }

        invoice.setStatut(StatutFacture.ANNULEE);
        Invoice saved = invoiceRepository.save(invoice);
        log.info("Facture id={} annulée avec succès", invoiceId);
        return invoiceMapper.toResponse(saved);
    }

    public void handleSubscriptionRenewed(SubscriptionRenewedEvent event) {
        log.info("Réception événement Kafka 'subscription-renewed' : subscriptionId={}, clientId={}, planId={}, periode={}",
                event.getSubscriptionId(), event.getClientId(), event.getPlanId(), event.getPeriode());

        if (invoiceRepository.existsBySubscriptionIdAndPeriode(event.getSubscriptionId(), event.getPeriode())) {
            log.warn("Idempotence : Facture déjà existante pour subscriptionId={} et periode={}. Événement ignoré.",
                    event.getSubscriptionId(), event.getPeriode());
            return;
        }

        PlanDto plan = null;
        try {
            plan = externalServicesClient.getPlan(event.getPlanId());
        } catch (Exception e) {
            log.warn("Impossible de récupérer les détails du plan id={} : {}", event.getPlanId(), e.getMessage());
        }

        String planNom = (plan != null && plan.getNom() != null) ? plan.getNom() : "Plan #" + event.getPlanId();
        BigDecimal prix = (plan != null && plan.getPrixMensuel() != null) ? plan.getPrixMensuel() : BigDecimal.ZERO;

        Invoice invoice = Invoice.builder()
                .subscriptionId(event.getSubscriptionId())
                .clientId(event.getClientId())
                .periode(event.getPeriode())
                .echeance(LocalDate.now().plusDays(30))
                .statut(StatutFacture.BROUILLON)
                .build();

        InvoiceLine line = InvoiceLine.builder()
                .libelle("Abonnement mensuel - " + planNom)
                .quantite(1)
                .prixUnitaire(prix)
                .build();

        invoice.addLine(line);
        invoice.recalculerMontants();

        Invoice saved = invoiceRepository.save(invoice);
        log.info("Facture BROUILLON créée avec succès via événement Kafka (id={})", saved.getId());
    }

    public void handlePaymentReceived(PaymentReceivedEvent event) {
        log.info("Réception événement Kafka 'payment-received' pour facture id={}", event.getInvoiceId());
        try {
            markAsPaid(event.getInvoiceId());
        } catch (Exception e) {
            log.error("Erreur lors du traitement de 'payment-received' pour invoiceId={} : {}",
                    event.getInvoiceId(), e.getMessage());
        }
    }

    public Invoice findEntityById(Long id) {
        return invoiceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Facture introuvable avec l'id : " + id));
    }
}
