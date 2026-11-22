package org.gmp.serviceclients.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

@FeignClient(name = "service-facturation", path = "/api/factures")
public interface FacturationClient {

    @GetMapping("/client/{clientId}")
    List<Object> getInvoicesByClient(@PathVariable("clientId") Long clientId);
}

