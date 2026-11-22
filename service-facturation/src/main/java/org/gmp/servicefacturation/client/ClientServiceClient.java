package org.gmp.servicefacturation.client;

import org.gmp.servicefacturation.dto.ClientDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(
        name = "service-clients",
        url = "${service-clients.url:http://localhost:8083}",
        fallback = ClientServiceFallback.class
)
public interface ClientServiceClient {

    @GetMapping("/api/clients/{id}")
    ClientDto getClientById(@PathVariable("id") Long id);
}
