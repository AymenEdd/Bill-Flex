package org.gmp.servicefacturation.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentReceivedEvent {

    private String eventId;
    private Long paymentId;
    private Long invoiceId;
    private BigDecimal montant;
    private LocalDate datePaiement;
}
