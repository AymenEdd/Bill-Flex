package org.gmp.serviceabonnements.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(
        name = "service-clients",
        url = "${clients.service.url}"
)
public interface ClientServiceClient {

    @GetMapping("/api/clients/{id}")
    ClientDto getClientById(@PathVariable("id") Long id);
}
