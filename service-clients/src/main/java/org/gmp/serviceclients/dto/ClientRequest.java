package org.gmp.serviceclients.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import org.gmp.serviceclients.enums.ModePaiement;

public record ClientRequest(
        @NotBlank
        String nom,

        @NotBlank
        @Email
        String email,

        /** Format TVA : 2 lettres majuscules + 11 chiffres. Ex : MA12345678901 */
        @Pattern(regexp = "^[A-Z]{2}[0-9]{11}$",
                 message = "Le numéro de TVA doit respecter le format : 2 lettres + 11 chiffres (ex: MA12345678901)")
        String numeroTVA,

        @NotNull
        ModePaiement modePaiement
) {}
