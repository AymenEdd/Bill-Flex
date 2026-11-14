package org.gmp.serviceabonnements.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public class PlanRequest {

    @NotBlank(message = "Le nom du plan est obligatoire")
    @Size(max = 100, message = "Le nom ne doit pas dépasser 100 caractères")
    private String nom;

    @NotNull(message = "Le prix mensuel est obligatoire")
    @DecimalMin(value = "0.0", inclusive = true,
            message = "Le prix doit être positif ou nul")
    @Digits(integer = 10, fraction = 2)
    private BigDecimal prixMensuel;

    @NotNull(message = "Le quota utilisateurs est obligatoire")
    @Min(value = 1, message = "Le quota doit être supérieur à zéro")
    private Integer quotaUtilisateurs;

    public String getNom() {
        return nom;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    public BigDecimal getPrixMensuel() {
        return prixMensuel;
    }

    public void setPrixMensuel(BigDecimal prixMensuel) {
        this.prixMensuel = prixMensuel;
    }

    public Integer getQuotaUtilisateurs() {
        return quotaUtilisateurs;
    }

    public void setQuotaUtilisateurs(Integer quotaUtilisateurs) {
        this.quotaUtilisateurs = quotaUtilisateurs;
    }
}
