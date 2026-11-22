package org.gmp.serviceabonnements.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.math.BigDecimal;
import java.time.LocalDate;

@JsonIgnoreProperties(ignoreUnknown = true)
public record InvoiceDto(
        Long id,
        Long subscriptionId,
        Long clientId,
        String periode,
        BigDecimal montantHT,
        BigDecimal montantTTC,
        BigDecimal tauxTva,
        LocalDate echeance,
        String statut
) {
}