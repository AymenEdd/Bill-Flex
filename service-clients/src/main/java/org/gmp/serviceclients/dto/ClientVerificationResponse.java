package org.gmp.serviceclients.dto;

import org.gmp.serviceclients.enums.ModePaiement;
import org.gmp.serviceclients.enums.StatutClient;

/**
 * Réponse de vérification utilisée par service-facturation.
 * Expose le numeroTVA et le modePaiement du client.
 */
public record ClientVerificationResponse(
        Long id,
        String nom,
        String email,
        String numeroTVA,
        ModePaiement modePaiement,
        StatutClient statut
) {}
