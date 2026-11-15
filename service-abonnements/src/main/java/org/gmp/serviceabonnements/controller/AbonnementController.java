package org.gmp.serviceabonnements.controller;

import org.gmp.serviceabonnements.dto.AbonnementRequest;
import org.gmp.serviceabonnements.dto.AbonnementResponse;
import org.gmp.serviceabonnements.service.AbonnementService;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/abonnements")
public class AbonnementController {

    private final AbonnementService abonnementService;

    public AbonnementController(
            AbonnementService abonnementService
    ) {
        this.abonnementService = abonnementService;
    }

    // Create subscription
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AbonnementResponse create(
            @Valid @RequestBody AbonnementRequest request
    ) {
        return abonnementService.create(request);
    }

    // Get all subscriptions
    @GetMapping
    public List<AbonnementResponse> findAll() {
        return abonnementService.findAll();
    }

    // Get subscription by ID
    @GetMapping("/{id}")
    public AbonnementResponse findById(
            @PathVariable Long id
    ) {
        return abonnementService.findById(id);
    }

    // Get subscriptions by client
    @GetMapping("/client/{clientId}")
    public List<AbonnementResponse> findByClientId(
            @PathVariable Long clientId
    ) {
        return abonnementService.findByClientId(clientId);
    }


    @PatchMapping("/{id}/suspend")
    public AbonnementResponse suspend(@PathVariable Long id) {
        return abonnementService.suspend(id);
    }

    @PatchMapping("/{id}/reactivate")
    public AbonnementResponse reactivate(@PathVariable Long id) {
        return abonnementService.reactivate(id);
    }

    @PatchMapping("/{id}/cancel")
    public AbonnementResponse cancel(@PathVariable Long id) {
        return abonnementService.cancel(id);
    }

}
