package org.gmp.serviceclients.models;

import jakarta.persistence.*;
import lombok.*;
import org.gmp.serviceclients.enums.ModePaiement;
import org.gmp.serviceclients.enums.StatutClient;

@Entity
@Table(name = "clients")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Client {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(unique = true)
    private String numeroTVA;

    @Enumerated(EnumType.STRING)
    private ModePaiement modePaiement;

    @Enumerated(EnumType.STRING)
    private StatutClient statut;
}