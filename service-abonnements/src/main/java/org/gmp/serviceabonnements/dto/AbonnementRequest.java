package org.gmp.serviceabonnements.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.LocalDate;

public class AbonnementRequest {

    @NotNull(message = "Client ID is required")
    @Positive(message = "Client ID must be positive")
    private Long clientId;

    @NotNull(message = "Plan ID is required")
    @Positive(message = "Plan ID must be positive")
    private Long planId;

    @NotNull(message = "Start date is required")
    private LocalDate dateDebut;

    private LocalDate dateFin;

    public Long getClientId() {
        return clientId;
    }

    public void setClientId(Long clientId) {
        this.clientId = clientId;
    }

    public Long getPlanId() {
        return planId;
    }

    public void setPlanId(Long planId) {
        this.planId = planId;
    }

    public LocalDate getDateDebut() {
        return dateDebut;
    }

    public void setDateDebut(LocalDate dateDebut) {
        this.dateDebut = dateDebut;
    }

    public LocalDate getDateFin() {
        return dateFin;
    }

    public void setDateFin(LocalDate dateFin) {
        this.dateFin = dateFin;
    }
}
