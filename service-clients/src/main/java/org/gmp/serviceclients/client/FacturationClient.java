package org.gmp.serviceclients.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

@FeignClient(name = "service-facturation", path = "/api/invoices")
public interface FacturationClient {

    /** V4 : récupère une facture par son id (pour vérifier l'existence avant paiement). */
    @GetMapping("/{id}")
    InvoiceDto getInvoiceById(@PathVariable("id") Long id);

    /** V4 : endpoint enrichi — toutes les factures d'un client. */
    @GetMapping("/client/{clientId}")
    List<InvoiceDto> getInvoicesByClient(@PathVariable("clientId") Long clientId);
}

