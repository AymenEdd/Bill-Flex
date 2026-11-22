package org.gmp.serviceclients.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.gmp.serviceclients.dto.PaymentRequest;
import org.gmp.serviceclients.dto.PaymentResponse;
import org.gmp.serviceclients.service.PaymentService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    /** V4 : création manuelle d'un paiement (vérifie que la facture existe via Feign). */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PaymentResponse create(@Valid @RequestBody PaymentRequest dto) {
        return paymentService.create(dto);
    }

    @GetMapping
    public List<PaymentResponse> findAll(
            @RequestParam(required = false) Long invoiceId) {
        return paymentService.findAll(invoiceId);
    }

    @GetMapping("/{id}")
    public PaymentResponse findById(@PathVariable Long id) {
        return paymentService.findById(id);
    }

    /**
     * V5 : PUT /api/payments/{id}/pay
     * Passe le paiement à PAYE et publie l'événement payment-received.
     */
    @PutMapping("/{id}/pay")
    public PaymentResponse pay(@PathVariable Long id) {
        return paymentService.pay(id);
    }
}
