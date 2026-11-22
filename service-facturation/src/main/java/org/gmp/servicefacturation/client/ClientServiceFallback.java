package org.gmp.servicefacturation.client;

import org.gmp.servicefacturation.dto.ClientDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class ClientServiceFallback implements ClientServiceClient {

    private static final Logger log = LoggerFactory.getLogger(ClientServiceFallback.class);

    @Override
    public ClientDto getClientById(Long id) {
        log.warn("Fallback déclenché pour getClientById({}) : service-clients indisponible", id);
        return ClientDto.builder()
                .id(id)
                .nom("Client Inconnu (Fallback)")
                .email("inconnu@fallback.local")
                .numeroTVA(null)
                .modePaiement(null)
                .statut("INDISPONIBLE")
                .build();
    }
}
