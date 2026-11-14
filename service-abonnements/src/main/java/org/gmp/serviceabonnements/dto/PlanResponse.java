package org.gmp.serviceabonnements.dto;

import java.math.BigDecimal;

public class PlanResponse {

    private Long id;
    private String nom;
    private BigDecimal prixMensuel;
    private Integer quotaUtilisateurs;
    private Boolean actif;

    public PlanResponse() {
    }

    public PlanResponse(Long id, String nom, BigDecimal prixMensuel,
                        Integer quotaUtilisateurs, Boolean actif) {
        this.id = id;
        this.nom = nom;
        this.prixMensuel = prixMensuel;
        this.quotaUtilisateurs = quotaUtilisateurs;
        this.actif = actif;
    }

    public Long getId() {
        return id;
    }

    public String getNom() {
        return nom;
    }

    public BigDecimal getPrixMensuel() {
        return prixMensuel;
    }

    public Integer getQuotaUtilisateurs() {
        return quotaUtilisateurs;
    }

    public Boolean getActif() {
        return actif;
    }
}

