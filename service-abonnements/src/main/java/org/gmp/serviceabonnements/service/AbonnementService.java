package org.gmp.serviceabonnements.service;

import org.gmp.serviceabonnements.dto.AbonnementRequest;
import org.gmp.serviceabonnements.dto.AbonnementResponse;
import org.gmp.serviceabonnements.entity.Abonnement;
import org.gmp.serviceabonnements.entity.Plan;
import org.gmp.serviceabonnements.entity.StatutAbonnement;
import org.gmp.serviceabonnements.mapper.AbonnementMapper;
import org.gmp.serviceabonnements.repository.AbonnementRepository;
import org.gmp.serviceabonnements.repository.PlanRepository;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import org.gmp.serviceabonnements.client.ClientDto;
import org.gmp.serviceabonnements.client.ClientServiceClient;

import java.util.List;

@Service
@Transactional
public class AbonnementService {

    private final AbonnementRepository abonnementRepository;
    private final PlanRepository planRepository;
    private final ClientServiceClient clientServiceClient;
    public AbonnementService(
            AbonnementRepository abonnementRepository,
            PlanRepository planRepository,
            ClientServiceClient clientServiceClient
    ) {
        this.abonnementRepository = abonnementRepository;
        this.planRepository = planRepository;
        this.clientServiceClient = clientServiceClient;
    }

    // Create a subscription
    public AbonnementResponse create(AbonnementRequest request) {

        Plan plan = planRepository.findById(request.getPlanId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Plan not found"
                ));

        if (!Boolean.TRUE.equals(plan.getActif())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "This plan is inactive"
            );
        }

        if (request.getDateFin() != null &&
                request.getDateFin().isBefore(request.getDateDebut())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "End date cannot be before start date"
            );
        }

        boolean alreadySubscribed =
                abonnementRepository.existsByClientIdAndStatut(
                        request.getClientId(),
                        StatutAbonnement.ACTIF
                );

        if (alreadySubscribed) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Client already has an active subscription"
            );
        }
        // Verify that the client exists
        ClientDto client = clientServiceClient.getClientById(
                request.getClientId()
        );

        if (client == null) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Client not found"
            );
        }

// Verify that the client is active
        if (client.getStatut() == null ||
                !client.getStatut().equalsIgnoreCase("Actif")) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Client is not active"
            );
        }
        Abonnement abonnement =
                AbonnementMapper.toEntity(request, plan);

        Abonnement saved =
                abonnementRepository.save(abonnement);

        return AbonnementMapper.toResponse(saved);
    }

    // Get all subscriptions
    @Transactional(readOnly = true)
    public List<AbonnementResponse> findAll() {
        return abonnementRepository.findAll()
                .stream()
                .map(AbonnementMapper::toResponse)
                .toList();
    }

    // Get subscriptions by client
    @Transactional(readOnly = true)
    public List<AbonnementResponse> findByClientId(Long clientId) {
        return abonnementRepository.findByClientId(clientId)
                .stream()
                .map(AbonnementMapper::toResponse)
                .toList();
    }

    // Get subscription by ID
    @Transactional(readOnly = true)
    public AbonnementResponse findById(Long id) {

        Abonnement abonnement = abonnementRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Subscription not found"
                ));

        return AbonnementMapper.toResponse(abonnement);
    }

    // Suspend a subscription
    public AbonnementResponse suspend(Long id) {
        Abonnement abonnement = getAbonnementOrThrow(id);

        if (abonnement.getStatut() != StatutAbonnement.ACTIF) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Only active subscriptions can be suspended"
            );
        }

        abonnement.setStatut(StatutAbonnement.SUSPENDU);

        return AbonnementMapper.toResponse(
                abonnementRepository.save(abonnement)
        );
    }

    // Reactivate a subscription
    public AbonnementResponse reactivate(Long id) {
        Abonnement abonnement = getAbonnementOrThrow(id);

        if (abonnement.getStatut() != StatutAbonnement.SUSPENDU) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Only suspended subscriptions can be reactivated"
            );
        }

        abonnement.setStatut(StatutAbonnement.ACTIF);

        return AbonnementMapper.toResponse(
                abonnementRepository.save(abonnement)
        );
    }

    // Cancel a subscription
    public AbonnementResponse cancel(Long id) {
        Abonnement abonnement = getAbonnementOrThrow(id);

        if (abonnement.getStatut() == StatutAbonnement.RESILIE) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Subscription is already cancelled"
            );
        }

        abonnement.setStatut(StatutAbonnement.RESILIE);
        abonnement.setDateFin(java.time.LocalDate.now());

        return AbonnementMapper.toResponse(
                abonnementRepository.save(abonnement)
        );
    }

    // Find subscription or return 404
    private Abonnement getAbonnementOrThrow(Long id) {
        return abonnementRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Subscription not found"
                ));
    }

}
