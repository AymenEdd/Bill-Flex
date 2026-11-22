package org.gmp.serviceclients.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.gmp.serviceclients.dto.ClientDetailsResponse;
import org.gmp.serviceclients.dto.ClientRequest;
import org.gmp.serviceclients.dto.ClientResponse;
import org.gmp.serviceclients.service.ClientService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/clients")
@RequiredArgsConstructor
public class ClientController {

    private final ClientService clientService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ClientResponse create(@Valid @RequestBody ClientRequest dto) {
        return clientService.create(dto);
    }

    @GetMapping
    public List<ClientResponse> findAll() {
        return clientService.findAll();
    }

    @GetMapping("/{id}")
    public ClientResponse findById(@PathVariable Long id) {
        return clientService.findById(id);
    }

    /** Client enrichi : ses données + ses factures (récupérées dans service-facturation via Feign). */
    @GetMapping("/{id}/details")
    public ClientDetailsResponse details(@PathVariable Long id) {
        return clientService.getDetails(id);
    }

    @PutMapping("/{id}")
    public ClientResponse update(@PathVariable Long id, @Valid @RequestBody ClientRequest dto) {
        return clientService.update(id, dto);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        clientService.delete(id);
    }
}