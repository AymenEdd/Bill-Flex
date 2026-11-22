package org.gmp.serviceabonnements.repository;

import org.gmp.serviceabonnements.entity.Abonnement;
import org.gmp.serviceabonnements.entity.StatutAbonnement;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AbonnementRepository
        extends JpaRepository<Abonnement, Long> {
    Optional<Abonnement> findByIdempotencyKey(String idempotencyKey);
    List<Abonnement> findByClientId(Long clientId);

    List<Abonnement> findByStatut(StatutAbonnement statut);

    boolean existsByClientIdAndStatut(
            Long clientId,
            StatutAbonnement statut
    );
    List<Abonnement> findByStatutAndDateFinLessThanEqual(
            StatutAbonnement statut,
            LocalDate date
    );
    List<Abonnement> findByStatutAndProchaineDateFacturationLessThanEqual(
            StatutAbonnement statut,
            LocalDate date
    );
    Page<Abonnement> findByClientId(Long clientId, Pageable pageable);

    Page<Abonnement> findByStatut(
            StatutAbonnement statut,
            Pageable pageable
    );

    Page<Abonnement> findByClientIdAndStatut(
            Long clientId,
            StatutAbonnement statut,
            Pageable pageable
    );
    long countByStatut(StatutAbonnement statut);
    List<Abonnement> findByClientIdOrderByDateDebutDesc(Long clientId);

    @Query("""
                SELECT a FROM Abonnement a
                JOIN a.plan p
                WHERE (:clientId IS NULL OR a.clientId = :clientId)
                  AND (:statut IS NULL OR a.statut = :statut)
                  AND (
                      :nomPlan = ''
                      OR LOWER(p.nom) LIKE LOWER(CONCAT('%', :nomPlan, '%'))
                  )
            """)
    Page<Abonnement> searchSubscriptions(
            @Param("clientId") Long clientId,
            @Param("statut") StatutAbonnement statut,
            @Param("nomPlan") String nomPlan,
            Pageable pageable
    );
    List<Abonnement> findByStatutAndDateFinBetween(
            StatutAbonnement statut,
            LocalDate startDate,
            LocalDate endDate
    );

}
