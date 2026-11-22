package org.gmp.serviceabonnements.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(
        name = "service-facturation",
        url = "${facturation.service.url}"
)
public interface FacturationServiceClient {

    @GetMapping("/api/invoices")
    List<InvoiceDto> getInvoicesBySubscription(@RequestParam("subscriptionId") Long subscriptionId);
}