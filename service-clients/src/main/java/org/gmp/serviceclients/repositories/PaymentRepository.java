package org.gmp.serviceclients.repositories;

import org.gmp.serviceclients.models.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    boolean existsByInvoiceId(Long invoiceId);

    List<Payment> findByInvoiceId(Long invoiceId);
}