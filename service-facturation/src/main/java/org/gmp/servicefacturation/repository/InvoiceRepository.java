package org.gmp.servicefacturation.repository;

import org.gmp.servicefacturation.entity.Invoice;
import org.gmp.servicefacturation.entity.StatutFacture;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, Long> {

    Optional<Invoice> findBySubscriptionIdAndPeriode(Long subscriptionId, String periode);

    boolean existsBySubscriptionIdAndPeriode(Long subscriptionId, String periode);

    List<Invoice> findByClientId(Long clientId);

    List<Invoice> findBySubscriptionId(Long subscriptionId);

    List<Invoice> findByStatut(StatutFacture statut);

    List<Invoice> findByStatutAndEcheanceBefore(StatutFacture statut, LocalDate date);
}
