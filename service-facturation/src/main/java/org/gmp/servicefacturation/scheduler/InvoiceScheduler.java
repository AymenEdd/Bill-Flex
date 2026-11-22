package org.gmp.servicefacturation.scheduler;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.gmp.servicefacturation.entity.Invoice;
import org.gmp.servicefacturation.entity.StatutFacture;
import org.gmp.servicefacturation.repository.InvoiceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

@Component
@RequiredArgsConstructor
public class InvoiceScheduler {

    private static final Logger log = LoggerFactory.getLogger(InvoiceScheduler.class);

    private final InvoiceRepository invoiceRepository;

    /**
     * Tâche planifiée exécutée quotidiennement pour détecter les factures impayées
     * dont la date d'échéance est dépassée et les passer au statut EN_RETARD.
     */
    @Scheduled(cron = "${invoices.scheduler.overdue.cron:0 0 1 * * *}")
    @Transactional
    public int checkAndMarkOverdueInvoices() {
        LocalDate today = LocalDate.now();
        log.info("Démarrage du traitement planifié des factures échues au {}", today);

        List<Invoice> overdueInvoices = invoiceRepository.findByStatutAndEcheanceBefore(
                StatutFacture.EMISE,
                today
        );

        if (overdueInvoices.isEmpty()) {
            log.info("Aucune facture échue à passer en retard.");
            return 0;
        }

        for (Invoice invoice : overdueInvoices) {
            invoice.setStatut(StatutFacture.EN_RETARD);
            log.warn("Facture id={} échue au {} passée au statut EN_RETARD",
                    invoice.getId(), invoice.getEcheance());
        }

        invoiceRepository.saveAll(overdueInvoices);
        log.info("Traitement terminé : {} factures passées en EN_RETARD", overdueInvoices.size());
        return overdueInvoices.size();
    }
}
