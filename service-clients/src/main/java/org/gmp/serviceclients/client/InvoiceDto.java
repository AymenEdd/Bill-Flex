package org.gmp.serviceclients.client;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * DTO léger représentant une facture retournée par service-facturation via Feign.
 */
public record InvoiceDto(
        Long id,
        Long clientId,
        Long abonnementId,
        BigDecimal montantHT,
        BigDecimal montantTTC,
        String statut,
        LocalDate dateEmission,
        LocalDate dateEcheance
) {}
