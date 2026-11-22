package org.gmp.serviceclients.dto;

import org.gmp.serviceclients.enums.ModePaiement;
import org.gmp.serviceclients.enums.StatutPaiement;

import java.math.BigDecimal;
import java.time.LocalDate;

public record PaymentResponse(
        Long id,
        Long invoiceId,
        Long clientId,
        LocalDate date,
        BigDecimal montant,
        ModePaiement moyen,
        StatutPaiement statut
) {}