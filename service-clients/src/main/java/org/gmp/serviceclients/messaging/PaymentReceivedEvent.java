package org.gmp.serviceclients.messaging;

import java.math.BigDecimal;
import java.time.LocalDate;

public record PaymentReceivedEvent(
        Long paymentId,
        Long invoiceId,
        Long clientId,
        BigDecimal montant,
        LocalDate date,
        String moyen
) {}