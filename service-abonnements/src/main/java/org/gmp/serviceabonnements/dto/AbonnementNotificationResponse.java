
package org.gmp.serviceabonnements.dto;

import java.time.LocalDate;

public class AbonnementNotificationResponse {

    private Long abonnementId;
    private Long clientId;
    private String nomPlan;
    private LocalDate dateFin;
    private long joursRestants;

    public AbonnementNotificationResponse(
            Long abonnementId,
            Long clientId,
            String nomPlan,
            LocalDate dateFin,
            long joursRestants) {
        this.abonnementId = abonnementId;
        this.clientId = clientId;
        this.nomPlan = nomPlan;
        this.dateFin = dateFin;
        this.joursRestants = joursRestants;
    }

    public Long getAbonnementId() {
        return abonnementId;
    }

    public Long getClientId() {
        return clientId;
    }

    public String getNomPlan() {
        return nomPlan;
    }

    public LocalDate getDateFin() {
        return dateFin;
    }

    public long getJoursRestants() {
        return joursRestants;
    }
}
