package org.gmp.serviceclients.dto;

import java.util.List;

public record ClientDetailsResponse(
        ClientResponse client,
        List<Object> factures
) {}

