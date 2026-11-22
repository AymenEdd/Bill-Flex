package org.gmp.servicefacturation.dto;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InvoiceLineResponse {

    private Long id;
    private Long invoiceId;
    private String libelle;
    private int quantite;
    private BigDecimal prixUnitaire;
    private BigDecimal totalLigne;
}
