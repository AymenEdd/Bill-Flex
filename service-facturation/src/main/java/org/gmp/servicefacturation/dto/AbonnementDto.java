package org.gmp.servicefacturation.dto;

import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AbonnementDto {

    private Long id;
    private Long clientId;
    private Long planId;
    private String nomPlan;
    private LocalDate dateDebut;
    private LocalDate dateFin;
    private LocalDate prochaineDateFacturation;
    private String statut;
}
