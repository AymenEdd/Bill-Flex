package org.gmp.servicefacturation.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InvoiceEnrichedResponse {

    private InvoiceResponse invoice;
    private ClientDto client;
    private AbonnementDto abonnement;
    private PlanDto plan;
}
