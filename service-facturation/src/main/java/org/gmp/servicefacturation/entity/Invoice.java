package org.gmp.servicefacturation.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
        name = "invoices",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_invoices_subscription_periode",
                        columnNames = {"subscription_id", "periode"}
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Invoice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "subscription_id", nullable = false)
    private Long subscriptionId;

    @Column(name = "client_id", nullable = false)
    private Long clientId;

    @Column(name = "periode", nullable = false, length = 50)
    private String periode;

    @Column(name = "montant_ht", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal montantHT = BigDecimal.ZERO;

    @Column(name = "montant_ttc", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal montantTTC = BigDecimal.ZERO;

    @Column(name = "taux_tva", precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal tauxTva = new BigDecimal("20.00");

    @Column(name = "echeance", nullable = false)
    private LocalDate echeance;

    @Enumerated(EnumType.STRING)
    @Column(name = "statut", nullable = false, length = 30)
    @Builder.Default
    private StatutFacture statut = StatutFacture.BROUILLON;

    @OneToMany(mappedBy = "invoice", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @Builder.Default
    private List<InvoiceLine> lines = new ArrayList<>();

    public void addLine(InvoiceLine line) {
        if (line != null) {
            lines.add(line);
            line.setInvoice(this);
        }
    }

    public void removeLine(InvoiceLine line) {
        if (line != null) {
            lines.remove(line);
            line.setInvoice(null);
        }
    }

    public void recalculerMontants() {
        BigDecimal totalHT = BigDecimal.ZERO;
        if (lines != null) {
            for (InvoiceLine line : lines) {
                totalHT = totalHT.add(line.calculerSousTotal());
            }
        }
        this.montantHT = totalHT;
        BigDecimal taux = (tauxTva != null) ? tauxTva : new BigDecimal("20.00");
        BigDecimal tvaMultiplier = BigDecimal.ONE.add(taux.divide(new BigDecimal("100.00"), 4, java.math.RoundingMode.HALF_UP));
        this.montantTTC = this.montantHT.multiply(tvaMultiplier).setScale(2, java.math.RoundingMode.HALF_UP);
    }
}
