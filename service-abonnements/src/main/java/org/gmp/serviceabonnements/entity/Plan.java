package org.gmp.serviceabonnements.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "plans", uniqueConstraints = {
        @UniqueConstraint(name = "uk_plan_nom", columnNames = "nom")
})
public class Plan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String nom;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal prixMensuel;

    @Column(nullable = false)
    private Integer quotaUtilisateurs;

    @Column(nullable = false)
    private Boolean actif = true;

    public Plan() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

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

    public Boolean getActif() {
        return actif;
    }

    public void setActif(Boolean actif) {
        this.actif = actif;
    }
}
