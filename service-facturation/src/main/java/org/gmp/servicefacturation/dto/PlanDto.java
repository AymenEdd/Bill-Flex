package org.gmp.servicefacturation.dto;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlanDto {

    private Long id;
    private String nom;
    private BigDecimal prixMensuel;
    private Integer quotaUtilisateurs;
    private Boolean actif;
}
