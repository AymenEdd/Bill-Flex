package org.gmp.serviceclients.dto;

import org.gmp.serviceclients.enums.ModePaiement;
import org.gmp.serviceclients.enums.StatutClient;

public class ClientResponse {
    Long id;
    String nom;
    String email;
    String numeroTVA;
    ModePaiement modePaiement;
    StatutClient statut;

    public ClientResponse(Long id, String name, String email, String numeroTVA, ModePaiement modePaiement, StatutClient statut) {
    }
}
