package org.gmp.serviceclients.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.gmp.serviceclients.enums.ModePaiement;

public record ClientRequest(
        @NotBlank
        String nom,

        @NotBlank
        @Email
        String email,

        String numeroTVA,

        @NotNull
        ModePaiement modePaiement
) {}