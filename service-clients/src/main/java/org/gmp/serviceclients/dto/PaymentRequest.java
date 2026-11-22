package org.gmp.serviceclients.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.gmp.serviceclients.enums.ModePaiement;

import java.math.BigDecimal;

/** montant et moyen optionnels : par défaut montantTTC de la facture / mode de paiement du client. */
public record PaymentRequest(
        @NotNull Long invoiceId,
        @Positive BigDecimal montant,
        ModePaiement moyen
) {}