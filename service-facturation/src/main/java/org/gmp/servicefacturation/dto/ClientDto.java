package org.gmp.servicefacturation.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClientDto {

    private Long id;
    private String nom;
    private String email;
    private String numeroTVA;
    private String modePaiement;
    private String statut;
}
