package org.gmp.serviceclients.models;

import jakarta.persistence.*;
import lombok.*;
import org.gmp.serviceclients.enums.ModePaiement;
import org.gmp.serviceclients.enums.StatutPaiement;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "payments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Idempotence : un seul paiement par facture. */
    @Column(nullable = false, unique = true)
    private Long invoiceId;

    @Column(nullable = false)
    private Long clientId;

    /** Date du règlement : renseignée au passage à PAYE. */
    private LocalDate date;

    @Column(precision = 19, scale = 2)
    private BigDecimal montant;

    @Enumerated(EnumType.STRING)
    private ModePaiement moyen;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatutPaiement statut;
}