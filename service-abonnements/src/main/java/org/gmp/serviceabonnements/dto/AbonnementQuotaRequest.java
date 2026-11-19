
package org.gmp.serviceabonnements.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public class AbonnementQuotaRequest {

    @NotNull
    @Positive
    private Integer nombreUtilisateurs;

    public Integer getNombreUtilisateurs() {
        return nombreUtilisateurs;
    }

    public void setNombreUtilisateurs(Integer nombreUtilisateurs) {
        this.nombreUtilisateurs = nombreUtilisateurs;
    }
}
