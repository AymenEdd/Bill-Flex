package org.gmp.serviceabonnements.dto;

import org.gmp.serviceabonnements.entity.StatutAbonnement;

import java.time.LocalDate;

public class AbonnementResponse {

    private Long id;
    private Long clientId;
    private Long planId;
    private String nomPlan;
    private LocalDate dateDebut;
    private LocalDate dateFin;
    private LocalDate prochaineDateFacturation;
    private StatutAbonnement statut;

    public AbonnementResponse(
            Long id,
            Long clientId,
            Long planId,
            String nomPlan,
            LocalDate dateDebut,
            LocalDate dateFin,
            LocalDate prochaineDateFacturation,
            StatutAbonnement statut
    ) {
        this.id = id;
        this.clientId = clientId;
        this.planId = planId;
        this.nomPlan = nomPlan;
        this.dateDebut = dateDebut;
        this.dateFin = dateFin;
        this.prochaineDateFacturation = prochaineDateFacturation;
        this.statut = statut;
    }

    public Long getId() {
        return id;
    }

    public Long getClientId() {
        return clientId;
    }

    public Long getPlanId() {
        return planId;
    }

    public String getNomPlan() {
        return nomPlan;
    }

    public LocalDate getDateDebut() {
        return dateDebut;
    }

    public LocalDate getDateFin() {
        return dateFin;
    }

    public LocalDate getProchaineDateFacturation() {
        return prochaineDateFacturation;
    }

    public StatutAbonnement getStatut() {
        return statut;
    }
}
