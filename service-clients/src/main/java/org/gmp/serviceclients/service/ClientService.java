package org.gmp.serviceclients.service;

import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.gmp.serviceclients.dto.ClientRequest;
import org.gmp.serviceclients.dto.ClientResponse;
import org.gmp.serviceclients.enums.StatutClient;
import org.gmp.serviceclients.models.Client;
import org.gmp.serviceclients.repositories.Clientrepository;
import org.springframework.stereotype.Service;


import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class ClientService {
   private final Clientrepository clientrepository;
    private StatutClient StatutClient;

    public ClientResponse create(@Valid ClientRequest dto){
       Client client = Client.builder()
               .name(dto.nom())
               .email(dto.email())
               .numeroTVA(dto.numeroTVA())
               .modePaiement(dto.modePaiement())
               .statut(StatutClient)
               .build();

       return toResponse(clientrepository.save(client));
   }


    @Transactional
    public List<ClientResponse> findAll() {
        return clientrepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public ClientResponse findById(Long id) {

        Client client = clientrepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("Client introuvable"));

        return toResponse(client);
    }
    public ClientResponse update(
            Long id, ClientRequest dto) {

        Client client = clientrepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("Client introuvable"));

        client.setName(dto.nom());
        client.setEmail(dto.email());
        client.setNumeroTVA(dto.numeroTVA());
        client.setModePaiement(dto.modePaiement());

        return toResponse(clientrepository.save(client));
    }

    public void delete(Long id) {

        Client client = clientrepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Client introuvable"));

        clientrepository.delete(client);
    }
    private ClientResponse toResponse(Client client) {

        return new ClientResponse(
                client.getId(),
                client.getName(),
                client.getEmail(),
                client.getNumeroTVA(),
                client.getModePaiement(),
                client.getStatut()
        );

    }
}


