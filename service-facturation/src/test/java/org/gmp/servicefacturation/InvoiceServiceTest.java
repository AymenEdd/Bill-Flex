package org.gmp.servicefacturation;

import org.gmp.servicefacturation.client.ExternalServicesClient;
import org.gmp.servicefacturation.dto.*;
import org.gmp.servicefacturation.entity.Invoice;
import org.gmp.servicefacturation.entity.InvoiceLine;
import org.gmp.servicefacturation.entity.StatutFacture;
import org.gmp.servicefacturation.exception.BusinessValidationException;
import org.gmp.servicefacturation.exception.IdempotencyConflictException;
import org.gmp.servicefacturation.exception.IllegalStateTransitionException;
import org.gmp.servicefacturation.kafka.InvoiceEventPublisher;
import org.gmp.servicefacturation.mapper.InvoiceMapper;
import org.gmp.servicefacturation.repository.InvoiceLineRepository;
import org.gmp.servicefacturation.repository.InvoiceRepository;
import org.gmp.servicefacturation.scheduler.InvoiceScheduler;
import org.gmp.servicefacturation.service.InvoiceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InvoiceServiceTest {

    @Mock
    private InvoiceRepository invoiceRepository;

    @Mock
    private InvoiceLineRepository invoiceLineRepository;

    @Mock
    private ExternalServicesClient externalServicesClient;

    @Mock
    private InvoiceEventPublisher invoiceEventPublisher;

    private InvoiceMapper invoiceMapper;
    private InvoiceService invoiceService;
    private InvoiceScheduler invoiceScheduler;

    @BeforeEach
    void setUp() {
        invoiceMapper = new InvoiceMapper();
        invoiceService = new InvoiceService(
                invoiceRepository,
                invoiceLineRepository,
                invoiceMapper,
                externalServicesClient,
                invoiceEventPublisher
        );
        invoiceScheduler = new InvoiceScheduler(invoiceRepository);
    }

    @Test
    @DisplayName("Calcul exact du montantHT et montantTTC avec TVA")
    void testCalculMontants() {
        Invoice invoice = Invoice.builder()
                .id(1L)
                .subscriptionId(10L)
                .clientId(20L)
                .periode("2026-11")
                .tauxTva(new BigDecimal("20.00"))
                .echeance(LocalDate.now().plusDays(30))
                .statut(StatutFacture.BROUILLON)
                .lines(new ArrayList<>())
                .build();

        invoice.addLine(InvoiceLine.builder().libelle("Licence").quantite(2).prixUnitaire(new BigDecimal("50.00")).build());
        invoice.addLine(InvoiceLine.builder().libelle("Support").quantite(1).prixUnitaire(new BigDecimal("100.00")).build());

        invoice.recalculerMontants();

        // 2 * 50 = 100, 1 * 100 = 100 => Total HT = 200.00
        assertEquals(new BigDecimal("200.00"), invoice.getMontantHT().setScale(2));
        // TTC = 200 * 1.20 = 240.00
        assertEquals(new BigDecimal("240.00"), invoice.getMontantTTC().setScale(2));
    }

    @Test
    @DisplayName("Création de facture avec contrainte d'idempotence respectée")
    void testCreateInvoice_Success() {
        InvoiceRequest request = InvoiceRequest.builder()
                .subscriptionId(1L)
                .clientId(2L)
                .periode("2026-11")
                .echeance(LocalDate.now().plusDays(30))
                .lines(List.of(
                        InvoiceLineRequest.builder().libelle("Plan Pro").quantite(1).prixUnitaire(new BigDecimal("49.99")).build()
                ))
                .build();

        when(invoiceRepository.existsBySubscriptionIdAndPeriode(1L, "2026-11")).thenReturn(false);
        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(invocation -> {
            Invoice inv = invocation.getArgument(0);
            inv.setId(100L);
            return inv;
        });

        InvoiceResponse response = invoiceService.createInvoice(request);

        assertNotNull(response);
        assertEquals(100L, response.getId());
        assertEquals(StatutFacture.BROUILLON, response.getStatut());
        assertEquals(new BigDecimal("49.99"), response.getMontantHT());
    }

    @Test
    @DisplayName("Rejet 409 si doublon pour subscriptionId + periode (Idempotence)")
    void testCreateInvoice_IdempotencyConflict() {
        InvoiceRequest request = InvoiceRequest.builder()
                .subscriptionId(1L)
                .clientId(2L)
                .periode("2026-11")
                .echeance(LocalDate.now().plusDays(30))
                .build();

        when(invoiceRepository.existsBySubscriptionIdAndPeriode(1L, "2026-11")).thenReturn(true);

        assertThrows(IdempotencyConflictException.class, () -> invoiceService.createInvoice(request));
    }

    @Test
    @DisplayName("Émission avec succès après vérification synchrone Feign")
    void testEmitInvoice_Success() {
        Invoice invoice = Invoice.builder()
                .id(1L)
                .subscriptionId(10L)
                .clientId(20L)
                .periode("2026-11")
                .echeance(LocalDate.now().plusDays(15))
                .statut(StatutFacture.BROUILLON)
                .lines(new ArrayList<>())
                .build();

        when(invoiceRepository.findById(1L)).thenReturn(Optional.of(invoice));
        when(externalServicesClient.getAbonnement(10L)).thenReturn(
                AbonnementDto.builder().id(10L).statut("ACTIF").build()
        );
        when(externalServicesClient.getClient(20L)).thenReturn(
                ClientDto.builder().id(20L).numeroTVA("FR123456789").modePaiement("CARTE").build()
        );
        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(i -> i.getArgument(0));

        InvoiceResponse response = invoiceService.emitInvoice(1L);

        assertEquals(StatutFacture.EMISE, response.getStatut());
        verify(invoiceEventPublisher, times(1)).publishInvoiceIssued(any(InvoiceIssuedEvent.class));
    }

    @Test
    @DisplayName("Refus d'émission si abonnement n'est pas actif")
    void testEmitInvoice_SubscriptionInactive() {
        Invoice invoice = Invoice.builder()
                .id(1L)
                .subscriptionId(10L)
                .clientId(20L)
                .periode("2026-11")
                .statut(StatutFacture.BROUILLON)
                .build();

        when(invoiceRepository.findById(1L)).thenReturn(Optional.of(invoice));
        when(externalServicesClient.getAbonnement(10L)).thenReturn(
                AbonnementDto.builder().id(10L).statut("SUSPENDU").build()
        );

        assertThrows(BusinessValidationException.class, () -> invoiceService.emitInvoice(1L));
        verify(invoiceEventPublisher, never()).publishInvoiceIssued(any());
    }

    @Test
    @DisplayName("Refus d'émission si client sans TVA ou mode de paiement")
    void testEmitInvoice_ClientInvalid() {
        Invoice invoice = Invoice.builder()
                .id(1L)
                .subscriptionId(10L)
                .clientId(20L)
                .periode("2026-11")
                .statut(StatutFacture.BROUILLON)
                .build();

        when(invoiceRepository.findById(1L)).thenReturn(Optional.of(invoice));
        when(externalServicesClient.getAbonnement(10L)).thenReturn(
                AbonnementDto.builder().id(10L).statut("ACTIF").build()
        );
        when(externalServicesClient.getClient(20L)).thenReturn(
                ClientDto.builder().id(20L).numeroTVA(null).modePaiement(null).build()
        );

        assertThrows(BusinessValidationException.class, () -> invoiceService.emitInvoice(1L));
        verify(invoiceEventPublisher, never()).publishInvoiceIssued(any());
    }

    @Test
    @DisplayName("Machine d'états : transition interdite renvoie 409")
    void testMachineEtats_TransitionsInterdites() {
        // Tentative de marquer PAYEE une facture en BROUILLON -> Interdit
        Invoice brouillon = Invoice.builder().id(1L).statut(StatutFacture.BROUILLON).build();
        when(invoiceRepository.findById(1L)).thenReturn(Optional.of(brouillon));
        assertThrows(IllegalStateTransitionException.class, () -> invoiceService.markAsPaid(1L));

        // Tentative d'annuler une facture déjà PAYEE -> Interdit
        Invoice payee = Invoice.builder().id(2L).statut(StatutFacture.PAYEE).build();
        when(invoiceRepository.findById(2L)).thenReturn(Optional.of(payee));
        assertThrows(IllegalStateTransitionException.class, () -> invoiceService.cancelInvoice(2L));

        // Tentative de passer en EN_RETARD une facture en BROUILLON -> Interdit
        when(invoiceRepository.findById(1L)).thenReturn(Optional.of(brouillon));
        assertThrows(IllegalStateTransitionException.class, () -> invoiceService.markAsOverdue(1L));
    }

    @Test
    @DisplayName("Consommation subscription-renewed avec création facture BROUILLON et idempotence")
    void testHandleSubscriptionRenewed_SuccessAndIdempotence() {
        SubscriptionRenewedEvent event = SubscriptionRenewedEvent.builder()
                .eventId("evt-123")
                .subscriptionId(5L)
                .clientId(10L)
                .planId(2L)
                .periode("2026-12")
                .build();

        when(invoiceRepository.existsBySubscriptionIdAndPeriode(5L, "2026-12")).thenReturn(false);
        when(externalServicesClient.getPlan(2L)).thenReturn(
                PlanDto.builder().id(2L).nom("Enterprise").prixMensuel(new BigDecimal("99.00")).build()
        );
        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(i -> i.getArgument(0));

        invoiceService.handleSubscriptionRenewed(event);

        ArgumentCaptor<Invoice> captor = ArgumentCaptor.forClass(Invoice.class);
        verify(invoiceRepository, times(1)).save(captor.capture());
        Invoice saved = captor.getValue();
        assertEquals(StatutFacture.BROUILLON, saved.getStatut());
        assertEquals(new BigDecimal("99.00"), saved.getMontantHT());
        assertEquals(1, saved.getLines().size());

        // Test idempotence : si le même événement est re-reçu
        when(invoiceRepository.existsBySubscriptionIdAndPeriode(5L, "2026-12")).thenReturn(true);
        invoiceService.handleSubscriptionRenewed(event);
        // Le compteur de save ne doit pas avoir augmenté
        verify(invoiceRepository, times(1)).save(any());
    }

    @Test
    @DisplayName("Consommation payment-received : passage à PAYEE")
    void testHandlePaymentReceived() {
        Invoice invoice = Invoice.builder().id(7L).statut(StatutFacture.EMISE).build();
        when(invoiceRepository.findById(7L)).thenReturn(Optional.of(invoice));
        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(i -> i.getArgument(0));

        PaymentReceivedEvent event = PaymentReceivedEvent.builder()
                .eventId("pay-1")
                .invoiceId(7L)
                .montant(new BigDecimal("100.00"))
                .build();

        invoiceService.handlePaymentReceived(event);

        assertEquals(StatutFacture.PAYEE, invoice.getStatut());
        verify(invoiceRepository, times(1)).save(invoice);
    }

    @Test
    @DisplayName("Passage automatique en EN_RETARD des factures échues par le scheduler")
    void testScheduler_OverdueInvoices() {
        Invoice inv1 = Invoice.builder().id(1L).statut(StatutFacture.EMISE).echeance(LocalDate.now().minusDays(2)).build();
        Invoice inv2 = Invoice.builder().id(2L).statut(StatutFacture.EMISE).echeance(LocalDate.now().minusDays(5)).build();

        when(invoiceRepository.findByStatutAndEcheanceBefore(eq(StatutFacture.EMISE), any(LocalDate.class)))
                .thenReturn(List.of(inv1, inv2));

        int count = invoiceScheduler.checkAndMarkOverdueInvoices();

        assertEquals(2, count);
        assertEquals(StatutFacture.EN_RETARD, inv1.getStatut());
        assertEquals(StatutFacture.EN_RETARD, inv2.getStatut());
        verify(invoiceRepository, times(1)).saveAll(any());
    }
}
