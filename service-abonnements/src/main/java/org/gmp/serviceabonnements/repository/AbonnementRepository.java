package org.gmp.serviceabonnements.repository;

import org.gmp.serviceabonnements.entity.Abonnement;
import org.gmp.serviceabonnements.entity.StatutAbonnement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AbonnementRepository
        extends JpaRepository<Abonnement, Long> {

    List<Abonnement> findByClientId(Long clientId);

    List<Abonnement> findByStatut(StatutAbonnement statut);

    boolean existsByClientIdAndStatut(
            Long clientId,
            StatutAbonnement statut
    );
}
