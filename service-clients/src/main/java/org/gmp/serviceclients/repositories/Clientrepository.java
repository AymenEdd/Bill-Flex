package org.gmp.serviceclients.repositories;

import org.gmp.serviceclients.models.Client;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface Clientrepository
        extends JpaRepository<Client, Long> {

    Optional<Client> findByEmail(String email);

    Boolean existsByNumeroTVA(String numeroTVA);
}
