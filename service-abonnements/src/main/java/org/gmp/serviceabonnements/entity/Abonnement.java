package org.gmp.serviceabonnements.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import jakarta.persistence.Column;
@Entity
@Table(name = "abonnements")
public class Abonnement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "idempotency_key", unique = true, length = 100)
    private String idempotencyKey;
    // Relation with Plan
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "plan_id", nullable = false)
    private Plan plan;

    // Client ID from service-clients
    @Column(name = "client_id", nullable = false)
    private Long clientId;

    @Column(name = "date_debut", nullable = false)
    private LocalDate dateDebut;

    @Column(name = "date_fin")
    private LocalDate dateFin;

    @Column(name = "prochaine_date_facturation")
    private LocalDate prochaineDateFacturation;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatutAbonnement statut = StatutAbonnement.ACTIF;

    @Column(nullable = false)
    private boolean expirationNotificationSent = false;

    @Version
    private Long version;

    public Abonnement() {
    }

    // Getters and setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Plan getPlan() {
        return plan;
    }

    public void setPlan(Plan plan) {
        this.plan = plan;
    }

    public Long getClientId() {
        return clientId;
    }

    public void setClientId(Long clientId) {
        this.clientId = clientId;
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

    public LocalDate getProchaineDateFacturation() {
        return prochaineDateFacturation;
    }

    public void setProchaineDateFacturation(
            LocalDate prochaineDateFacturation) {
        this.prochaineDateFacturation = prochaineDateFacturation;
    }

    public StatutAbonnement getStatut() {
        return statut;
    }

    public void setStatut(StatutAbonnement statut) {
        this.statut = statut;
    }

    public Long getVersion() {
        return version;
    }

    public boolean isExpirationNotificationSent() {
        return expirationNotificationSent;
    }

    public void setExpirationNotificationSent(boolean expirationNotificationSent) {
        this.expirationNotificationSent = expirationNotificationSent;
    }
    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public void setIdempotencyKey(String idempotencyKey) {
        this.idempotencyKey = idempotencyKey;
    }
}
