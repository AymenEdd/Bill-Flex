package org.gmp.servicefacturation.dto;

import lombok.*;
import org.gmp.servicefacturation.entity.StatutFacture;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InvoiceResponse {

    private Long id;
    private Long subscriptionId;
    private Long clientId;
    private String periode;
    private BigDecimal montantHT;
    private BigDecimal montantTTC;
    private BigDecimal tauxTva;
    private LocalDate echeance;
    private StatutFacture statut;
    private List<InvoiceLineResponse> lines;
}
