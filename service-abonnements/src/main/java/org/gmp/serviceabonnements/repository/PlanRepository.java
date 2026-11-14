package org.gmp.serviceabonnements.repository;

import org.gmp.serviceabonnements.entity.Plan;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PlanRepository extends JpaRepository<Plan, Long> {

    boolean existsByNom(String nom);

    List<Plan> findByActifTrue();
}
