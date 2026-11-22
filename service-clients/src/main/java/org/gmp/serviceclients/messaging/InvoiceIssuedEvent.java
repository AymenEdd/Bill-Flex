package org.gmp.serviceclients.messaging;

import java.math.BigDecimal;
import java.time.LocalDate;

public record InvoiceIssuedEvent(
        Long invoiceId,
        Long clientId,
        BigDecimal montantTTC,
        LocalDate echeance
) {}