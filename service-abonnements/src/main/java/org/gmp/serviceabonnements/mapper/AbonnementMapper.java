package org.gmp.serviceabonnements.mapper;

import org.gmp.serviceabonnements.dto.AbonnementRequest;
import org.gmp.serviceabonnements.dto.AbonnementResponse;
import org.gmp.serviceabonnements.entity.Abonnement;
import org.gmp.serviceabonnements.entity.Plan;

import java.time.LocalDate;

public class AbonnementMapper {

    public static Abonnement toEntity(
            AbonnementRequest request,
            Plan plan
    ) {
        Abonnement abonnement = new Abonnement();

        abonnement.setClientId(request.getClientId());
        abonnement.setPlan(plan);
        abonnement.setDateDebut(request.getDateDebut());
        abonnement.setDateFin(request.getDateFin());
        abonnement.setStatut(
                org.gmp.serviceabonnements.entity.StatutAbonnement.ACTIF
        );

        // First billing date = subscription start date + 1 month
        abonnement.setProchaineDateFacturation(
                request.getDateDebut().plusMonths(1)
        );

        return abonnement;
    }

    public static AbonnementResponse toResponse(
            Abonnement abonnement
    ) {
        return new AbonnementResponse(
                abonnement.getId(),
                abonnement.getClientId(),
                abonnement.getPlan().getId(),
                abonnement.getPlan().getNom(),
                abonnement.getDateDebut(),
                abonnement.getDateFin(),
                abonnement.getProchaineDateFacturation(),
                abonnement.getStatut()
        );
    }
}
