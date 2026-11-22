package org.gmp.servicefacturation.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InvoiceRequest {

    @NotNull(message = "L'ID de l'abonnement (subscriptionId) est obligatoire")
    private Long subscriptionId;

    @NotNull(message = "L'ID du client (clientId) est obligatoire")
    private Long clientId;

    @NotBlank(message = "La période est obligatoire (ex: 2026-11)")
    private String periode;

    @NotNull(message = "La date d'échéance est obligatoire")
    private LocalDate echeance;

    private BigDecimal tauxTva;

    @Valid
    private List<InvoiceLineRequest> lines;
}
