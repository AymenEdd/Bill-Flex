package org.gmp.serviceclients.dto;

import org.gmp.serviceclients.client.InvoiceDto;

import java.util.List;

public record ClientDetailsResponse(
        ClientResponse client,
        List<InvoiceDto> factures
) {}
