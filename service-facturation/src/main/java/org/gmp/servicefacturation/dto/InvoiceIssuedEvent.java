package org.gmp.servicefacturation.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InvoiceIssuedEvent {

    private String eventId;
    private Long invoiceId;
    private Long subscriptionId;
    private Long clientId;
    private String periode;
    private BigDecimal montantHT;
    private BigDecimal montantTTC;
    private LocalDate echeance;
    private LocalDateTime dateEmission;
}
