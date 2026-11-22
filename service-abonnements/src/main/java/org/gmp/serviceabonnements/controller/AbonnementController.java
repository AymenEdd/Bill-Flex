package org.gmp.serviceabonnements.controller;

import org.gmp.serviceabonnements.dto.*;
import org.gmp.serviceabonnements.entity.StatutAbonnement;
import org.gmp.serviceabonnements.service.AbonnementService;

import jakarta.validation.Valid;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import org.springframework.http.ResponseEntity;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.RequestHeader;
import org.gmp.serviceabonnements.client.ClientDto;
@RestController
@RequestMapping({"/api/abonnements", "/api/subscriptions", "/subscriptions"})
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
            @Valid @RequestBody AbonnementRequest request,
            @RequestHeader("Idempotency-Key") String idempotencyKey) {

        return abonnementService.create(request, idempotencyKey);
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
    @GetMapping("/page")
    public Page<AbonnementResponse> findAllPaginated(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id,desc") String[] sort) {

        Pageable pageable = PageRequest.of(
                page,
                size,
                Sort.by(
                        Sort.Direction.fromString(sort[1]),
                        sort[0]
                )
        );

        return abonnementService.findAllPaginated(pageable);
    }
    @GetMapping("/filter")
    public Page<AbonnementResponse> findFiltered(
            @RequestParam(required = false) Long clientId,
            @RequestParam(required = false) StatutAbonnement statut,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(
                page,
                size,
                Sort.by("id").descending()
        );

        return abonnementService.findFiltered(clientId, statut, pageable);
    }
    @GetMapping("/statistics")
    public AbonnementStatisticsResponse getStatistics() {
        return abonnementService.getStatistics();
    }
    @GetMapping("/client/{clientId}/history")
    public List<AbonnementResponse> getClientHistory(
            @PathVariable Long clientId) {

        return abonnementService.getClientHistory(clientId);
    }
    @PostMapping("/{id}/renew")
    public AbonnementResponse renew(@PathVariable Long id) {
        return abonnementService.renew(id);
    }
    @GetMapping("/search")
    public Page<AbonnementResponse> searchSubscriptions(
            @RequestParam(required = false) Long clientId,
            @RequestParam(required = false) StatutAbonnement statut,
            @RequestParam(required = false) String nomPlan,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(
                page,
                size,
                Sort.by("id").descending()
        );

        return abonnementService.searchSubscriptions(
                clientId,
                statut,
                nomPlan,
                pageable
        );
    }
    @PutMapping("/{id}")
    public AbonnementResponse update(
            @PathVariable Long id,
            @Valid @RequestBody AbonnementUpdateRequest request) {

        return abonnementService.update(id, request);
    }


    @PostMapping("/{id}/validate-quota")
    public ResponseEntity<Void> validateQuota(
            @PathVariable Long id,
            @Valid @RequestBody AbonnementQuotaRequest request) {

        abonnementService.validateQuota(
                id,
                request.getNombreUtilisateurs()
        );

        return ResponseEntity.noContent().build();
    }
    @GetMapping("/notifications/upcoming-expirations")
    public List<AbonnementNotificationResponse> getUpcomingExpirations() {
        return abonnementService.getUpcomingExpirations();
    }
    @PostMapping("/notifications/send")
    public ResponseEntity<String> sendExpirationNotifications() {

        int sent = abonnementService.sendExpirationNotifications();

        return ResponseEntity.ok(
                sent + " expiration notification(s) sent successfully"
        );
    }
    // Get complete subscription details
    @GetMapping("/{id}/details")
    public AbonnementDetailsResponse getAbonnementDetails(
            @PathVariable Long id) {

        return abonnementService.getAbonnementDetails(id);
    }

    // Get complete subscription details with invoices (V4 aggregation: 2 Feign calls)
    @GetMapping("/{id}/full-details")
    public AbonnementFullDetailsResponse getAbonnementFullDetails(
            @PathVariable Long id) {

        return abonnementService.getAbonnementFullDetails(id);
    }

    @GetMapping("/clients")
    public List<ClientDto> getAllClients() {
        return abonnementService.getAllClients();
    }

}
