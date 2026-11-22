package org.gmp.service_notifications.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "service-clients", url = "${notifications.customer.url:http://localhost:8083}")
public interface CustomerClient {

    @GetMapping("/api/clients/{id}")
    CustomerResponse findById(@PathVariable("id") Long id);

    @JsonIgnoreProperties(ignoreUnknown = true)
    record CustomerResponse(Long id, String email) {
    }
}
